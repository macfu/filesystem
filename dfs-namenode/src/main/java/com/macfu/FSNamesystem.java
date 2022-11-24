package com.macfu;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 负责管理元数据的核心组件
 */
@Slf4j
public class FSNamesystem {

    // 副本数量
    public static final Integer REPLICA_NUM = 2;
    /**
     * 负责管理内存文件目录树的结构
     * 专门负责维护内存中的文件目录树
     */
    private FSDirectory directory;
    /**
     * 负责管理edits log写入磁盘的组件
     */
    private FSEditlog editlog;
    /**
     * 最近一次checkpoint更新到的txid
     */
    private long checkpointTxid = 0;
    /**
     * 每个文件对应的副本所在的datanode
     */
    private Map<String, List<DataNodeInfo>> replicaByFilename = new HashMap<>();
    /**
     * 每个DataNode对应的所有文件的副本
     */
    private Map<String, List<String>> filesByDatanode = new HashMap<>();
    /**
     * 副本数据机构的锁
     */
    ReentrantReadWriteLock replicasLock = new ReentrantReadWriteLock();
    /**
     * 数据节点管理组件
     */
    private DataNodeManager dataNodeManager;

    public FSNamesystem(DataNodeManager dataNodeManager) {
        this.directory = new FSDirectory();
        this.editlog = new FSEditlog(this);
        this.dataNodeManager = dataNodeManager;
        revoverNamespace();
    }

    /**
     * 创建目录
     * @param path
     * @return
     * @throws Exception
     */
    public Boolean mkdir(String path) throws Exception {
        directory.mkDir(path);
        editlog.logEdit(EditLogFactory.mkdir(path));
        return true;
    }

    /**
     * 创建文件
     * @param filename 文件名，包含所在的绝对路径【/products/img001.jpg】
     * @return
     * @throws Exception
     */
    public Boolean create(String filename) throws Exception {
        if (!directory.create(filename)) {
            return false;
        }
        editlog.logEdit(EditLogFactory.create(filename));
        return true;
    }

    /**
     * 强制把内存里的edits log刷入磁盘中
     */
    public void flush() {
        this.editlog.flush();
    }

    /**
     * 获取一个edit log组件
     * @return
     */
    public FSEditlog getEditlog() {
        return editlog;
    }


    public void setCheckpointTxid(long txid) {
        log.info("接收到的checkpoint txid：" + txid);
        this.checkpointTxid = txid;
    }

    public long getCheckpointTxid() {
        return checkpointTxid;
    }

    /**
     * 将checkpoint txid保存到磁盘上去
     */
    public void saveCheckpointTxid() {
        String path = "F:\\development\\editslog\\checkpoint-txid.meta";

        RandomAccessFile raf = null;
        FileOutputStream out = null;
        FileChannel channel = null;
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }

            ByteBuffer buffer = ByteBuffer.wrap(String.valueOf(checkpointTxid).getBytes());
            raf = new RandomAccessFile(path, "rw");
            out = new FileOutputStream(raf.getFD());
            channel = out.getChannel();
            channel.write(buffer);
            channel.force(false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (raf != null){
                    raf.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

    }

    /**
     * 恢复元数据
     */
    public void revoverNamespace() {
        try {
            loadFSImage();
            loadCheckpointTxid();
            loadEditLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载fsimage文件到内存里来进行恢复
     * @throws Exception
     */
    private void loadFSImage() throws Exception {
        FileInputStream in = null;
        FileChannel channel = null;
        try {
            String path = "F:\\development\\editslog\\fsimage.meta";
            File file = new File(path);
            if (!file.exists()) {
                log.info("fsimage文件当前不存在，不进行恢复......");
                return;
            }
            in = new FileInputStream(path);
            channel = in.getChannel();

            /**
             * 这个参数是可以动态调整的
             * 每次你接受到一个fsimage文件的时候记录一下他的大小，持久化到磁盘上去
             * 每次重启就分配对应空间大小就可以了
             */
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            int count = channel.read(buffer);

            buffer.flip();
            String fsimageJson = new String(buffer.array(), 0, count);
            log.info("恢复fsimage文件中的数据：" + fsimageJson);
            FSDirectory.INode dirTree = JSONObject.parseObject(fsimageJson, FSDirectory.INode.class);
            directory.setDirTree(dirTree);

        } finally {
            if (in != null) {
                in.close();
            }
            if (channel != null) {
                channel.close();
            }
        }
    }

    /**
     * 加载和回放editlog
     * @throws Exception
     */
    private void loadEditLog() throws Exception {
        File dir = new File("F:\\development\\editslog\\");

        List<File> files = Lists.newArrayList();
        for (File file : dir.listFiles()) {
            files.add(file);
        }

        Iterator<File> fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (!file.getName().contains("edits")) {
                fileIterator.remove();
            }
        }

        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                Integer olStartTxid = Integer.valueOf(o1.getName().split("-")[1]);
                Integer o2StartTxid = Integer.valueOf(o2.getName().split("-")[1]);
                return olStartTxid - o2StartTxid;
            }
        });

        if (CollectionUtils.isEmpty(files)) {
            log.info("当前没有任何editlog文件，不进行恢复");
            return;
        }

        for (File file : files) {
            if (file.getName().contains("edits")) {
                log.info("准备恢复editlog文件中的数据:" + file.getName());

                String[] splitedName  = file.getName().split("-");
                long starTxid = Long.valueOf(splitedName[1]);
                long endTxid  = Long.valueOf(splitedName[2].split("[.]")[0]);

                // 如果是checkpointTxid之后的那些editlog都要加载出来
                if (endTxid > checkpointTxid) {
                    String currenteditsLogFile = "F:\\development\\editslog\\edits-" + starTxid + "-" + endTxid + ".log";
                    List<String> editsLogs = Files.readAllLines(Paths.get(currenteditsLogFile), StandardCharsets.UTF_8);

                    for (String editLogJson : editsLogs) {
                        JSONObject editLog = JSONObject.parseObject(editLogJson);
                        long txid = editLog.getLongValue("txid");
                        if (txid > checkpointTxid) {
                            System.out.println("准备回放editlog：" + editLogJson);
                            // 回放到内存里去
                            String op = editLog.getString("OP");
                            if (op.equals("MKDIR")) {
                                String path = editLog.getString("PAHT");
                                try {
                                    directory.mkDir(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if (op.equals("CREATE")) {
                                String path = editLog.getString("PATH");
                                try {
                                    directory.create(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 加载checkpoint txid
     * @throws Exception
     */
    private void loadCheckpointTxid() throws Exception {
        FileInputStream in = null;
        FileChannel channel = null;
        try {
            String path = "F:\\development\\editslog\\checkpoint-txid.meta";
            File file = new File(path);
            if (!file.exists()) {
                log.info("checkpoint txid文件不存在，不进行恢复......");
                return;
            }
            in = new FileInputStream(path);
            channel = in.getChannel();

            // 这个参数可以动态调节
            /**
             * 每次你接收到一个fsimage文件的时候可以记录一下他的大小，持久化到磁盘
             * 每次重启就可以分配对应的大小
             */
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int read = channel.read(buffer);
            buffer.flip();
            long checkpointTxid = Long.valueOf(new String(buffer.array(), 0, read));
            log.info("恢复checkpoint txid：" + checkpointTxid);

            this.checkpointTxid = checkpointTxid;
        } finally {
            if (in != null) {
                in.close();
            }
            if (channel != null) {
                channel.close();
            }
        }
    }

    /**
     * 从数据节点删除一个文件副本
     * @param id
     * @param file
     */
    public void removeReplicaFromDataNode(String id, String file) {
        try {
            replicasLock.writeLock().lock();
            filesByDatanode.get(id).remove(file);
            Iterator<DataNodeInfo> replicasIterator = replicaByFilename.get(file.split("_")[0]).iterator();
            while (replicasIterator.hasNext()) {
                DataNodeInfo replica = replicasIterator.next();
                if (replica.getId().equals(id)) {
                    replicasIterator.remove();
                }
            }
        } finally {
            replicasLock.writeLock().unlock();
        }
    }

    /**
     * 给指定的文件增加一个成功接收的文件副本
     * @param hostname
     * @param ip
     * @param filename
     * @param fileLength
     */
    public void addReceivedReplica(String hostname, String ip, String filename, long fileLength) {
        try {
            replicasLock.writeLock().lock();
            DataNodeInfo dataNodeInfo = dataNodeManager.getDataNode(ip, hostname);
            List<DataNodeInfo> replicas = replicaByFilename.get(filename);
            if (replicas == null) {
                replicas = new ArrayList<>();
                replicaByFilename.put(filename, replicas);
            }
            // 检查当前文件的副本数量是否超标
            if (replicas.size() == REPLICA_NUM) {
                dataNodeInfo.setStoredDataSize(-fileLength);
                // 删除副本任务
                RemoveReplicaTask removeReplicaTask = new RemoveReplicaTask(filename, dataNodeInfo);
                dataNodeInfo.addRemoveReplicaTask(removeReplicaTask);
                return;
            }
            // 如果副本数量未超标，才会将副本放入数据结构中
            replicas.add(dataNodeInfo);

            List<String> files = filesByDatanode.get(ip + "-" + hostname);
            if (files == null) {
                files = new ArrayList<>();
                filesByDatanode.put(ip + "-" + hostname, files);
            }
            files.add(filename + "_" + fileLength);
            log.info("收到存储上报，当前的副本信息为：" + replicaByFilename + "." + filesByDatanode);
        } finally {
            replicasLock.writeLock().unlock();
        }
    }

    /**
     * 删除数据节点的文件副本数据结构
     * @param datanode
     */
    public void removedDeadDataNode(DataNodeInfo datanode) {
        try {
            replicasLock.writeLock().lock();
            List<String> filenames = filesByDatanode.get(datanode.getId());
            for (String filename : filenames) {
                List<DataNodeInfo> replicas = replicaByFilename.get(filename.split("_")[0]);
                replicas.remove(datanode);
            }

            filesByDatanode.remove(datanode.getId());
            log.info("从内存数据结构中删除掉这个数据节点关联的数据：" + replicaByFilename + "." + filesByDatanode);
        } finally {
            replicasLock.writeLock().unlock();
        }
    }

    /**
     * 获取节点包含的文件
     * @param ip
     * @param hostname
     * @return
     */
    public List<String> getFilesByDataNode(String ip, String hostname) {
        try {
            replicasLock.readLock().lock();
            log.info("当前filesByDatanode为：" + filesByDatanode
                    + ".将要以key=" + ip + "-" + hostname + "获取文件列表");

            return filesByDatanode.get(ip + "-" + hostname);
        } finally {
            replicasLock.readLock().unlock();
        }
    }

    /**
     * 获取复制任务的源头数据节点
     * @param filename
     * @param dataNodeInfo
     * @return
     */
    public DataNodeInfo getReplicateSource(String filename, DataNodeInfo dataNodeInfo) {
        DataNodeInfo replicateSource = null;
        try {
            replicasLock.readLock().lock();
            List<DataNodeInfo> replicas = replicaByFilename.get(filename);
            for (DataNodeInfo replica : replicas) {
                if (!replica.equals(dataNodeInfo)) {
                    replicateSource = replica;
                }
            }
        } finally {
            replicasLock.readLock().unlock();
        }
        return replicateSource;
    }

    /**
     * 获取文件的某个副本所在的机器
     * @param filename
     * @param excludeDataNodeId
     * @return
     */
    private DataNodeInfo chooseDataNodeFromReplicas(String filename, String excludeDataNodeId) {
        try {
            replicasLock.readLock().lock();
            DataNodeInfo excludedDataNode = dataNodeManager.getDataNode(excludeDataNodeId);
            List<DataNodeInfo> datanodes = replicaByFilename.get(filename);
            if (datanodes.size() == 1) {
                if (datanodes.get(0).equals(excludedDataNode)) {
                    return null;
                }
            }
            int size = datanodes.size();
            Random random = new Random();
            while (true) {
                int index = random.nextInt(size);
                DataNodeInfo dataNodeInfo = datanodes.get(index);
                if (!dataNodeInfo.equals(excludedDataNode)) {
                    return dataNodeInfo;
                }
            }
        } finally {
            replicasLock.readLock().unlock();
        }
    }
}






























