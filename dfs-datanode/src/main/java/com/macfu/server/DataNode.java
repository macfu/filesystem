package com.macfu.server;

import com.macfu.server.nio.NioServer;
import com.macfu.server.util.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * DataNode 启动类
 */
@Slf4j
public class DataNode {

    /**
     * 是否还在运行
     */
    private volatile Boolean shouldRun;
    /**
     * 负责跟一组namenode通信的组件
     */
    private NameNodeRpcClient nameNodeRpcClient;
    /**
     * 心跳管理组件
     */
    private HeartbeatManager heartbeatManager;
    /**
     * 磁盘存储管理组件
     */
    private StorageManager storageManager;
    /**
     * 复制任务管理组件
     */
    private ReplicateManager replicateManager;

    public DataNode() throws Exception {
        this.shouldRun = true;
        this.nameNodeRpcClient = new NameNodeRpcClient();
        Boolean registerResult = this.nameNodeRpcClient.register();

        this.storageManager = new StorageManager();
        if (registerResult) {
            StorageInfo storageInfo = this.storageManager.getStorageInfo();
            this.nameNodeRpcClient.reportCompleteStorageInfo(storageInfo);
        } else {
            log.info("不需要全量上报存储信息");
        }

        this.replicateManager = new ReplicateManager(this.nameNodeRpcClient);
        this.heartbeatManager = new HeartbeatManager(this.nameNodeRpcClient, this.storageManager, this.replicateManager);

        this.heartbeatManager.start();

        NioServer nioServer = new NioServer(nameNodeRpcClient);
        nioServer.init();
        nioServer.start();
    }

    /**
     * 运行datanode
     */
    private void start() {
        try {
            while (shouldRun) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(DataNodeConfig.DATA_DIR);
        DataNode dataNode = new DataNode();
        dataNode.start();
    }

}
