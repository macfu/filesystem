package com.macfu.server;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.macfu.dfs.namenode.rpc.model.HeartbeatResponse;
import com.macfu.server.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 心跳管理组件
 */
@Slf4j
public class HeartbeatManager {

    public static final Integer SUCCESS = 1;
    public static final Integer FAILURE = 2;
    public static final Integer COMMAND_REGISTER = 1;
    public static final Integer COMMAND_REPORT_COMPLETE_STORAGE_INFO = 2;
    public static final Integer COMMAND_REPLICATE = 3;
    public static final Integer COMMAND_REMOVE_REPLICA = 4;

    private NameNodeRpcClient nameNodeRpcClient;
    private StorageManager storageManager;
    private ReplicateManager replicateManager;

    public HeartbeatManager(NameNodeRpcClient nameNodeRpcClient, StorageManager storageManager, ReplicateManager replicateManager) {
        this.nameNodeRpcClient = nameNodeRpcClient;
        this.storageManager = storageManager;
        this.replicateManager = replicateManager;
    }

    public void start() {
        new HeartbeatThread().start();
    }

    /**
     * 负责心跳的线程
     */
    class HeartbeatThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    // 通过rpc接口发送到namenode他的注册接口上去
                    HeartbeatResponse response = nameNodeRpcClient.heartbeat();

                    if (SUCCESS.equals(response.getStatus())) {
                        JSONArray commands = JSONArray.parseArray(response.getCommands());

                        if (commands.size() > 0) {
                            for (int i = 0; i < commands.size(); i++) {
                                JSONObject command = commands.getJSONObject(i);
                                Integer type = command.getInteger("type");
                                JSONObject task = command.getJSONObject("content");

                                if (type.equals(COMMAND_REPLICATE)) {
                                    replicateManager.addReplicateTask(task);
                                    log.info("接收到副本复制任务：" + command);
                                } else if (type.equals(COMMAND_REMOVE_REPLICA)) {
                                    // 删除副本
                                    String filename = task.getString("filename");
                                    String absoluteFilename = FileUtils.getAbsoluteFilename(filename);
                                    File file = new File(absoluteFilename);
                                    if (file.exists()) {
                                        file.delete();
                                    }
                                }
                            }
                        }
                    }

                    if (FAILURE.equals(response.getStatus())) {
                        JSONArray commands = JSONArray.parseArray(response.getCommands());
                        for (int i = 0; i < commands.size(); i++) {
                            JSONObject command = commands.getJSONObject(i);
                            Integer type = command.getInteger("type");

                            if (type.equals(COMMAND_REGISTER)) {
                                nameNodeRpcClient.register();
                            } else if (type.equals(COMMAND_REPORT_COMPLETE_STORAGE_INFO)) {
                                StorageInfo storageInfo = storageManager.getStorageInfo();
                                nameNodeRpcClient.reportCompleteStorageInfo(storageInfo);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.info("当前namenode不可用，心跳失败......");
                }

                try {
                    // 每隔30s发送一次心跳到namenode上
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
