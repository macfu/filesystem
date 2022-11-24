package com.macfu;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.util.List;

/**
 * 负责管理edit log日志的核心组件
 */
@Slf4j
public class FSEditlog {

    // editlog日志文件清理的时间间隔
    private static final Long EDIT_LOG_CLEAN_INTERVAL = 30 * 1000L;
    // 元数据管理组件
    private FSNamesystem namesystem;
    /**
     * 当前递增的txid的序号
     */
    private long txidSeq = 0L;
    /**
     * 内存双缓冲区
     */
    private DoubleBuffer doubleBuffer = new DoubleBuffer();
    /**
     * 当前是否在将内存换成功刷入到磁盘中
     */
    private volatile Boolean isSyncRunning = false;
    /**
     * 在同步到磁盘中的最大的一个txid
     */
    private volatile Long syncTxid = 0L;
    /**
     * 是否正在调度一次刷盘的操作
     */
    private volatile Boolean isSchedulingSync = false;
    /**
     * 每个线程自己本地的txid副本
     */
    private ThreadLocal<Long> localTxid = new ThreadLocal<>();

    public FSEditlog(FSNamesystem fsNamesystem) {
        this.namesystem = fsNamesystem;
        EditLogCleaner editLogCleaner = new EditLogCleaner();
        editLogCleaner.start();
    }

    /**
     * 记录edit log日志
     * @param content
     */
    public void logEdit(String content) {
        synchronized (this) {
            waitSchedulingSync();
            // 获取全局唯一递增的id，代表了edit log的序号
            txidSeq++;
            long txid = txidSeq;
            // 放到ThreadLocal里去，相当于就是维护了一份本地线程副本
            localTxid.set(txid);
            Editlog log = new Editlog(txid, content);
            // 将edit log写入内存缓冲中，不是直接刷入磁盘文件
            try {
                doubleBuffer.write(log);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 每次写完一条消息之后，就应该判断一下当前这个缓冲区是否满了
            if (!doubleBuffer.shouldSyncToDisk()) {
                return;
            }
            // 如果缓冲区到了限制大小，设置刷盘表示为true
            isSchedulingSync = true;
        }
        logSync();
    }

    /**
     * 等待正在调度的刷盘操作完成
     */
    private void waitSchedulingSync() {
        try {
            while (isSchedulingSync) {
                /**
                 * 此时释放锁
                 */
                wait(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将内存缓冲中的数据刷入磁盘文件中
     * 在这里尝试允许某一个线程一次性将内存缓冲中的数据刷入磁盘文件中
     * 相当于实现一个批量将内存缓冲数据刷磁盘的过程
     */
    public void logSync() {
        synchronized (this) {
            // 获取到本地线程副本
            long txid = localTxid.get();
            // 如果说当前正好有人在刷内存缓冲到磁盘中去
            if (isSyncRunning) {
                /**
                 * 假如说某个线程已经把txid = 1，2,3,4,5的edit log都从syncBuffer刷入磁盘了
                 * 或者说此时正在刷入磁盘
                 * 此时syncMaxTxid = 5,代表的是正在刷入磁盘的最大txid
                 * 那么这个时候来一个线程，他对应的txid=3，此时直接返回【他对应的edit log已经被别的县城刷入磁盘了】
                 * 这个txid =3的线程就不需要等待了
                 */
                if (txid <= syncTxid) {
                    /**
                     * 如果之前有人在同步syncTxid=30的数据了
                     * 现在一个线程的txid=25，此时之后返回就可以了
                     */
                    return;
                }
                try {
                    while (isSyncRunning) {
                        wait(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 交换两个缓冲区
            doubleBuffer.setReadyToSync();
            /**
             * 保存一下当前要同步到磁盘中区的最大的txid
             * 此时editlogbuffer中的syncBuffer这块区域，交换完以后这里可能有多条数据
             * 而且他里面的edit log的txid一定是从小到大的
             * 此时要同步的txid=6,7,8,9，10,11,12
             * syncMantTxid = 12
             */
            syncTxid = txid;
            /**
             * syncTxid代表的就是说，把txid=30为止的之前所有的editlog都会刷入磁盘中
             * editlog的txid都是依次递增的，就是当前他的这个txid
             * 设置当前正在同步到磁盘的标志位
             */
            isSchedulingSync = false;
            notifyAll();
            isSyncRunning = true;
        }
        /**
         * 开始同步内存缓冲的数据到磁盘文件里去
         */
        try {
            doubleBuffer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (this) {
            // 同步完了磁盘之后，就会将标志位复位
            isSyncRunning = false;
            // 唤醒可能正在等待同步完磁盘的线程
            notifyAll();
        }
    }

    /**
     * 强制把内存里的数据输入磁盘中
     */
    public void flush() {
        try {
            doubleBuffer.setReadyToSync();
            doubleBuffer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取已经刷入磁盘的editlog数据
     * @return
     */
    public List<String> getFlushedTxid() {
        return doubleBuffer.getFlushedTxids();
    }

    /**
     * 获取当前缓冲区里的数据
     * @return
     */
    public String[] getBufferedEditsLog() {
        synchronized (this) {
            return doubleBuffer.getBufferedEditLog();
        }
    }

    /**
     * 自动清理editlog文件
     */
    class EditLogCleaner extends Thread {

        @Override
        public void run() {
            log.info("editlog日志文件后台清理线程启动。。。。。。");
            while (true) {
                try {
                    Thread.sleep(EDIT_LOG_CLEAN_INTERVAL);
                    List<String> flushedTxids = getFlushedTxid();
                    if (CollectionUtils.isNotEmpty(flushedTxids)) {
                        long checkPointTxid = namesystem.getCheckpointTxid();
                        // 检查点的txid已经大于当前这个日志文件上次写入磁盘的txid
                        for (String flushedTxid : flushedTxids) {
                            long startTxid = Long.valueOf(flushedTxid.split("_")[0]);
                            long endTxid = Long.valueOf(flushedTxid.split("_")[1]);
                            if (checkPointTxid >= endTxid) {
                                File file = new File("F:\\\\development\\\\editslog\\\\edits-" + startTxid + "-" + endTxid + ".log");
                                if (file.exists()) {
                                    file.delete();
                                    log.info("发现editlog日志文件不需要，继续删除：" + file.getPath());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
