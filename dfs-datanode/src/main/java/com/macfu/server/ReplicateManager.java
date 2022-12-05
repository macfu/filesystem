package com.macfu.server;

import com.alibaba.fastjson.JSONObject;
import com.macfu.server.nio.NioClient;
import com.macfu.server.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 副本复制管理组件
 */
@Slf4j
public class ReplicateManager {

    public static final Integer REPLICATE_THREAD_NUM = 3;

    private NioClient nioClient;
    private NameNodeRpcClient nameNodeRpcClient;

    /**
     * 副本复制任务队列
     */
    private ConcurrentLinkedQueue<JSONObject> replicateTaskQueue = new ConcurrentLinkedQueue<>();

    public ReplicateManager(NameNodeRpcClient nameNodeRpcClient) {
        this.nameNodeRpcClient = nameNodeRpcClient;
        for (int i = 0; i < REPLICATE_THREAD_NUM; i++) {
            new ReplicateWorker().start();
        }
    }

    public void addReplicateTask(JSONObject replicateTask) {
        replicateTaskQueue.offer(replicateTask);
    }

    /**
     * 副本复制线程
     */
    class ReplicateWorker extends Thread {

        @Override
        public void run() {
            while (true) {
                FileOutputStream imageOut = null;
                FileChannel imageChannel = null;
                try {
                    JSONObject replicateTask = replicateTaskQueue.poll();
                    if (replicateTask == null) {
                        Thread.sleep(1000);
                        continue;
                    }
                    log.info("开始执行副本复制任务......");

                    String filename = replicateTask.getString("filename");
                    Long fileLength = replicateTask.getLong("fileLength");

                    JSONObject sourceDatanode = replicateTask.getJSONObject("sourceDatanode");
                    String hostname = sourceDatanode.getString("hostname");
                    Integer nioPort = sourceDatanode.getInteger("nioPort");

                    // 跟源数据接头读取图片
                    byte[] file = nioClient.readFile(hostname, nioPort, filename);
                    ByteBuffer fileBuffer = ByteBuffer.wrap(file);
                    log.info("从源数据节点读取到图片，大小为：" + file.length + "字节");

                    String absoluteFilename = FileUtils.getAbsoluteFilename(filename);
                    imageOut = new FileOutputStream(absoluteFilename);
                    imageChannel = imageOut.getChannel();
                    imageChannel.write(fileBuffer);
                    log.info("将图片写入本地磁盘文件，路径为：" + absoluteFilename);

                    nameNodeRpcClient.informReplicaReceived(filename + "_" + fileLength);
                    log.info("向master节点进行增量上报");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        imageChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        imageOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
