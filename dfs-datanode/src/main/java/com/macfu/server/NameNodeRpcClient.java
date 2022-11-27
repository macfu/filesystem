package com.macfu.server;

import com.alibaba.fastjson.JSONArray;
import com.macfu.dfs.namenode.rpc.model.*;
import com.macfu.dfs.namenode.rpc.service.NameNodeServiceGrpc;
import com.macfu.server.util.DataNodeConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 负责跟一组namenode中的某一个进行通信的线程组件
 */
@Slf4j
public class NameNodeRpcClient {

    private NameNodeServiceGrpc.NameNodeServiceBlockingStub namenode;

    public NameNodeRpcClient() {
        ManagedChannel channel = NettyChannelBuilder
                .forAddress(DataNodeConfig.NAMENODE_HOSTNAME, DataNodeConfig.NAMENODE_PORT)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        this.namenode = NameNodeServiceGrpc.newBlockingStub(channel);
        log.info("跟namenode的" + DataNodeConfig.NAMENODE_PORT + "端口建立连接......");
    }

    /**
     * 向自己负责通信的那个namenode通信
     * @return
     * @throws Exception
     */
    public Boolean register() throws Exception {
        /**
         * 发送rpc接口调用请求到NameNode去进行注册
         * 注册的时候会发送那些信息呢？如：ip地址，hostname
         */
        RegisterRequest request = RegisterRequest.newBuilder()
                .setIp(DataNodeConfig.DATANODE_IP)
                .setHostname(DataNodeConfig.DATANODE_HOSTNAME)
                .setNioPort(DataNodeConfig.NIO_PORT)
                .build();
        RegisterResponse response = namenode.register(request);
        log.info("注册时：" + DataNodeConfig.DATA_DIR);
        log.info("完成向NameNode的注册，响应消息为：" + response.getStatus());
        if (response.getStatus() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送心跳
     * @return
     * @throws Exception
     */
    public HeartbeatResponse heartbeat() throws Exception {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setIp(DataNodeConfig.DATANODE_IP)
                .setHostname(DataNodeConfig.DATANODE_HOSTNAME)
                .setNioPort(DataNodeConfig.NIO_PORT)
                .build();
        return namenode.heartbeat(request);
    }

    /**
     * 上报全量存储信息
     * @param storageInfo
     */
    public void reportCompleteStorageInfo(StorageInfo storageInfo) {
        if (Objects.isNull(storageInfo)) {
            log.info("当前没有存储任何文件，不需要全量上传......");
            return;
        }

        ReportCompleteStorageInfoRequest request = ReportCompleteStorageInfoRequest.newBuilder()
                .setIp(DataNodeConfig.DATANODE_IP)
                .setHostname(DataNodeConfig.DATANODE_HOSTNAME)
                .setFilenames(JSONArray.toJSONString(storageInfo.getFilenames()))
                .build();

        namenode.reportCompleteStorageInfo(request);
        log.info("全量上报存储信息：" + storageInfo);

    }

    /**
     * 通知master（namenode）节点自己收到了一个文件的副本
     * @param filename
     * @throws Exception
     */
    public void informReplicaReceived(String filename) throws Exception {
        InformReplicaReceivedRequest request = InformReplicaReceivedRequest.newBuilder()
                .setHostname(DataNodeConfig.DATANODE_HOSTNAME)
                .setIp(DataNodeConfig.DATANODE_IP)
                .setFilename(filename).build();
        namenode.informReplicaReceived(request);
        log.info("通知namenode节点接收到一个文件：" + filename);
    }
}
