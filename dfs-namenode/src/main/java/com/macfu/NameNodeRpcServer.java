package com.macfu;

import com.macfu.dfs.namenode.rpc.service.NameNodeServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class NameNodeRpcServer {

    private static final int DEAFULT_PORT = 50070;

    private Server server = null;
    // 负责管理元数据的核心组件
    private FSNamesystem namesystem;
    // 负责管理集群中所有的datanode组件
    private DataNodeManager dataNodeManager;

    public NameNodeRpcServer(FSNamesystem namesystem, DataNodeManager dataNodeManager) {
        this.namesystem = namesystem;
        this.dataNodeManager = dataNodeManager;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(DEAFULT_PORT)
                .addService(NameNodeServiceGrpc.bindService(new Name))
    }
}
