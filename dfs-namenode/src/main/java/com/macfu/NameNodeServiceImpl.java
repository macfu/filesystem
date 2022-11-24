package com.macfu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.macfu.dfs.namenode.rpc.model.*;
import com.macfu.dfs.namenode.rpc.service.NameNodeServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * nameNode的rpc服务接口
 */
@Slf4j
public class NameNodeServiceImpl implements NameNodeServiceGrpc.NameNodeService {

    public static final Integer STATUS_SUCCESS = 1;
    public static final Integer STATUS_FAILURE = 2;
    public static final Integer STATUS_SHUTDOWN = 3;
    public static final Integer STATUS_DUPLICATE = 4;

    public static final Integer BACKUP_NODE_FETCH_SIZE = 10;
    // 负责管理元数据的核心组件
    /**
     * 是一个逻辑上的组件，主要负责管理元数据的更新
     * 比如说更新内存里的问津目录数的话，就可以去找他，他更新的是元数据
     */
    private FSNamesystem namesystem;
    // 负责管理集群中的所有的datanode组件
    private DataNodeManager dataNodeManager;
    // 是否还在运行
    private volatile Boolean isRunning = true;
    // 当前缓冲的一小部分edtislog
    private JSONArray currentBufferedEditsLog = new JSONArray();
    // 当前缓冲里 的editslog最大的一个txid
    private long currentBufferedMaxTxid = 0L;
    // 当前内存缓冲了哪个磁盘文件的数据
    private String bufferedFlushedTxid;

    public NameNodeServiceImpl(
            FSNamesystem namesystem,
            DataNodeManager dataNodeManager) {
        this.namesystem = namesystem;
        this.dataNodeManager = dataNodeManager;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        Boolean result = dataNodeManager.register(request.getIp(), request.getHostname(), request.getNioPort());
        RegisterResponse response = null;
        if (result) {
            response = RegisterResponse.newBuilder().setStatus(STATUS_SUCCESS).build();
        } else {
            response = RegisterResponse.newBuilder().setStatus(STATUS_FAILURE).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        String ip = request.getIp();
        String hostname = request.getHostname();
        Boolean result = dataNodeManager.heartBeat(ip, hostname);

        HeartbeatResponse response = null;
        List<Command> commands = Lists.newArrayList();

        if (result) {
            /**
             * 如果成功心跳，此时应该查看一下是否有复制副本的任务
             * 如果有，则做成命令下发给这个数据节点
             */
            DataNodeInfo dataNodeInfo = dataNodeManager.getDataNode(ip, hostname);
            ReplicateTask replicateTask = null;
            while ((replicateTask = dataNodeInfo.pollReplicateTask()) != null) {
                Command replicateCommand = new Command(Command.REPLICATE);
                replicateCommand.setContent(JSONObject.toJSONString(replicateCommand));
                commands.add(replicateCommand);
            }

            RemoveReplicaTask removeReplicaTask = null;
            while ((removeReplicaTask = dataNodeInfo.pollRemoveReplicaTask()) != null) {
                Command removeReplicaCommand = new Command(Command.REMOVE_REPLICA);
                removeReplicaCommand.setContent(JSONObject.toJSONString(removeReplicaCommand));
                commands.add(removeReplicaCommand);
            }

            log.info("接收到数据节点【" + dataNodeInfo + "】的心跳，他的命令列表为：" + commands);

            response = HeartbeatResponse.newBuilder()
                    .setStatus(STATUS_SUCCESS)
                    .setCommands(JSONArray.toJSONString(commands))
                    .build();
        } else {
            Command registerCommand = new Command(Command.REGISTER);
            Command reportCompleteStorageInfoCommand = new Command(Command.REPORT_COMPLETE_STORAGE_INFO);
            commands.add(registerCommand);
            commands.add(reportCompleteStorageInfoCommand);

            response = HeartbeatResponse.newBuilder()
                    .setStatus(STATUS_FAILURE)
                    .setCommands(JSONArray.toJSONString(commands))
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void mkdir(MkdirRequest request, StreamObserver<MkdirResponse> responseObserver) {
        try {
            MkdirResponse response = null;
            if (!isRunning) {
                response = MkdirResponse.newBuilder()
                        .setStatus(STATUS_SHUTDOWN)
                        .build();
            } else {
                this.namesystem.mkdir(request.getPath());

                response = MkdirResponse.newBuilder()
                        .setStatus(STATUS_SUCCESS)
                        .build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown(ShutdownRequest request, StreamObserver<ShutdownResponse> responseObserver) {
        isRunning = false;
        namesystem.flush();
        namesystem.saveCheckpointTxid();
        log.info("优雅关闭namenode......");
    }

    @Override
    public void fetchEditsLog(FetchEditsLogRequest request, StreamObserver<FetchEditsLogResponse> responseObserver) {
        if (!isRunning) {
            FetchEditsLogResponse response = FetchEditsLogResponse.newBuilder()
                    .setEditsLog(new JSONArray().toJSONString())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        long syncedTxid = request.getSyncedTxid();

        FetchEditsLogResponse response = null;
        JSONArray fetcheditsLog = new JSONArray();

        List<String> flushedTxids = namesystem.getEditlog().getFlushedTxid();
        /**
         * 如果此时还没有刷出来任何磁盘文件的话，那么此时数据仅仅存在内存缓冲里
         */
        if (flushedTxids.size() == 0) {
            fetchFromBufferedEditsLog(syncedTxid, fetcheditsLog);
        } else {
            /**
             * 如果此时发现已经有落地磁盘的文件了，这个时候就要扫描所有的磁盘文件的索引范围
             * <br>第一种情况：你要拉取的txid是在某个磁盘文件里</>
             * <br>有哦磁盘文件，而且内存里还缓存了某个磁盘文件的数据了</>
             */
            if (bufferedFlushedTxid != null) {
                // 如果要拉取的数据就在当前缓存的磁盘文件数据里面
                if (existInFlushedFile(syncedTxid, bufferedFlushedTxid)) {
                    fetchFromCurrentBuffer(syncedTxid, fetcheditsLog);
                } else {
                    // 如果要拉取的数据不在当前缓存的磁盘文件数据里了，那么就需要从下一个磁盘文件去拉取
                    String nextFlushedTxid = getNextFlushedTxid(flushedTxids, bufferedFlushedTxid);
                    // 如果可以找到下一个磁盘文件，那么就从下一个磁盘文件里开始读取数据
                    if (nextFlushedTxid != null) {
                        fetchFromFlushedFile(syncedTxid, nextFlushedTxid, fetcheditsLog);
                    } else {
                        // 如果没有找到下一个文件，此时就需要从内存里去继续读取
                        fetchFromBufferedEditsLog(syncedTxid, fetcheditsLog);
                    }
                }
            } else {
                // 第一次尝试从磁盘文件里去拉取
                Boolean fetchedFromFlushedFile = false;
                for (String flushedTxid : flushedTxids) {
                    // 如果要拉取的下一条数据就是在某个磁盘文件里
                    if (existInFlushedFile(syncedTxid, flushedTxid)) {
                        fetchFromFlushedFile(syncedTxid, flushedTxid, fetcheditsLog);
                        fetchedFromFlushedFile = true;
                        break;
                    }
                }

                /**
                 * 第二种情况，你要拉取的txid已经比磁盘文件里的全部都要新，还在内存缓冲里
                 * 如果没有找到下一个文件，此时就需要从内存里去继续读取
                 */
                if (!fetchedFromFlushedFile) {
                    fetchFromBufferedEditsLog(syncedTxid, fetcheditsLog);
                }
            }
        }

        response = FetchEditsLogResponse.newBuilder()
                .setEditsLog(fetcheditsLog.toJSONString())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateCheckpointTxid(UpdateCheckpointTxidRequest request, StreamObserver<UpdateCheckpointTxidResponse> responseObserver) {
        long txid = request.getTxid();
        namesystem.setCheckpointTxid(txid);

        UpdateCheckpointTxidResponse response = UpdateCheckpointTxidResponse.newBuilder()
                .setStatus(1)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void create(CreateFileRequest request, StreamObserver<CreateFileResponse> responseObserver) {
        /**
         * 把文件民的查重和创建文件放在一起执行
         * 如果说有很多歌客户端万一同时要发起文件创建，都有一个文件名过来
         * 多线程并发情况下，文件名的查重和创建都是正确执行的
         * 就必须得在同步的代码块来执行这个功能逻辑
         */
        try {
            CreateFileResponse response = null;
            if (!isRunning) {
                response = CreateFileResponse.newBuilder()
                        .setStatus(STATUS_FAILURE)
                        .build();
            } else {
                String filename = request.getFilename();
                Boolean success = namesystem.create(filename);
                if (success) {
                    response = CreateFileResponse.newBuilder().setStatus(STATUS_SUCCESS).build();
                } else {
                    response = CreateFileResponse.newBuilder()
                            .setStatus(STATUS_DUPLICATE)
                            .build();
                }
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 为文件上传请求分配多个数据节点来传输多个副本
     * @param request
     * @param responseObserver
     */
    @Override
    public void allocateDataNodes(AllocateDataNodesRequest request, StreamObserver<AllocateDataNodesResponse> responseObserver) {
        long fileSize = request.getFileSize();
        List<DataNodeInfo> datanodes = dataNodeManager.allocateDataNodes(fileSize);
        String datanodeJson = JSONArray.toJSONString(datanodes);

        AllocateDataNodesResponse response = AllocateDataNodesResponse.newBuilder()
                .setDatanodes(datanodeJson)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void informReplicaReceived(InformReplicaReceivedRequest request, StreamObserver<InformReplicaReceivedResponse> responseObserver) {

    }

    @Override
    public void reportCompleteStorageInfo(ReportCompleteStorageInfoRequest request, StreamObserver<ReportCompleteStorageInfoResponse> responseObserver) {

    }

    @Override
    public void chooseDataNodeFromReplicas(ChooseDataNodeFromReplicasRequest request, StreamObserver<ChooseDataNodeFromReplicasResponse> responseObserver) {

    }

    @Override
    public void reallocateDataNode(ReallocateDataNodeRequest request, StreamObserver<ReallocateDataNodeResponse> responseObserver) {

    }

    @Override
    public void rebalance(RebalanceRequest request, StreamObserver<RebalanceResponse> responseObserver) {

    }

    /**
     * 从内存缓冲里的editlog中拉取数据
     * @param syncedTxid
     * @param fetchedEditsLog
     */
    private void fetchFromBufferedEditsLog(long syncedTxid, JSONArray fetchedEditsLog) {
        // 如果要拉取的txid还在上一次内存缓存中，此时继续从内存缓冲来拉取即可
        long fetchTxid = syncedTxid + 1;
        if (fetchTxid <= currentBufferedMaxTxid) {
            fetchFromCurrentBuffer(syncedTxid, fetchedEditsLog);
            return;
        }
        currentBufferedEditsLog.clear();

        String[] bufferedEditsLog = namesystem.getEditlog().getBufferedEditsLog();
        if (bufferedEditsLog != null) {
            for (String editsLog : bufferedEditsLog) {
                currentBufferedEditsLog.add(JSONObject.parseObject(editsLog));
                /**
                 * 在这里记录一下，当前内存缓冲中的数据最大的一个txid是多少，这样下次再拉取可以判断
                 * ，内存缓冲里的额数据是否还可以读取，不要每次都重新从内存缓冲里加载
                 */
                currentBufferedMaxTxid = JSONObject.parseObject(editsLog).getLong("txid");
            }
            bufferedFlushedTxid = null;
            fetchFromCurrentBuffer(syncedTxid, fetchedEditsLog);
        }
    }

    /**
     * 从当前已经在内存里缓冲的数据中拉取edtislog
     * @param syncedTxid
     * @param fetchedEditsLog
     */
    private void fetchFromCurrentBuffer(long syncedTxid, JSONArray fetchedEditsLog) {
        int fetchCount = 0;
        long fetchTxid = syncedTxid + 1;

        for (int i = 0; i < currentBufferedEditsLog.size(); i++) {
            if (currentBufferedEditsLog.getJSONObject(i).getLong("txid") == fetchTxid) {
                fetchedEditsLog.add(currentBufferedEditsLog.getJSONObject(i));
                fetchTxid = currentBufferedEditsLog.getJSONObject(i).getLong("txid") + 1;
                fetchCount++;
            }
            if (fetchCount == BACKUP_NODE_FETCH_SIZE) {
                break;
            }
        }
    }

    /**
     * 是否存在于刷到磁盘的文件中
     * @param syncedTxid
     * @param flushTxid
     * @return
     */
    private Boolean existInFlushedFile(long syncedTxid, String flushTxid) {
        String [] flushedTxidSplited = flushTxid.split("_");

        long startTxid = Long.valueOf(flushedTxidSplited[0]);
        long endTxid = Long.valueOf(flushedTxidSplited[1]);
        long fetchTxid = syncedTxid + 1;

        if (fetchTxid >= startTxid && fetchTxid <= endTxid) {
            return true;
        }
        return false;
    }

    /**
     * 获取下一个磁盘文件对应的txid范围
     * @param flushedTxids
     * @param bufferedFlushedTxid
     * @return
     */
    private String getNextFlushedTxid(List<String> flushedTxids, String bufferedFlushedTxid) {
        for (int i = 0; i <flushedTxids.size(); i++) {
            if (flushedTxids.get(i).equals(bufferedFlushedTxid)) {
                if (i+1 < flushedTxids.size()) {
                    return flushedTxids.get(i+1);
                }
            }
        }
        return null;
    }

    /**
     * 从已经刷入磁盘的文件里读取editslog，同时缓存这个文件数据到内存
     * @param syncedTxid
     * @param flushedTxid
     * @param fetchedEditsLog
     */
    private void fetchFromFlushedFile(long syncedTxid, String flushedTxid, JSONArray fetchedEditsLog) {
        try {
            String[] flushedTxidSplited = flushedTxid.split("_");
            long startTxid = Long.valueOf(flushedTxidSplited[0]);
            long endTxid = Long.valueOf(flushedTxidSplited[1]);

            String currentEditLogFile = "F:\\development\\editslog-" + startTxid + "-" + endTxid + ".log";

            List<String> editsLogs = Files.readAllLines(Paths.get(currentEditLogFile), StandardCharsets.UTF_8);
            currentBufferedEditsLog.clear();
            for (String editsLog : editsLogs) {
                currentBufferedEditsLog.add(JSONObject.parseObject(editsLog));
                currentBufferedMaxTxid = JSONObject.parseObject(editsLog).getLong("txid");
            }
            // 缓存了某个磁盘文件的数据
            bufferedFlushedTxid = flushedTxid;

            fetchFromCurrentBuffer(syncedTxid, fetchedEditsLog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
