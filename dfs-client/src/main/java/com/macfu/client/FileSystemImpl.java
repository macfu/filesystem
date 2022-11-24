package com.macfu.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.macfu.dfs.namenode.rpc.model.*;
import com.macfu.dfs.namenode.rpc.service.NameNodeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件系统客户端的实现类
 */
@Slf4j
public class FileSystemImpl implements FileSystem {

    private static final String NAMENODE_HOSTNAME = "localhost";
    private static final Integer NAMENODE_PORT = 50070;

    private NameNodeServiceGrpc.NameNodeServiceBlockingStub namenode;
    private NioClient nioClient;

    public FileSystemImpl() {
        ManagedChannel channel = NettyChannelBuilder
                .forAddress(NAMENODE_HOSTNAME, NAMENODE_PORT)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        this.namenode = NameNodeServiceGrpc.newBlockingStub(channel);
        this.nioClient = new NioClient();
    }

    /**
     * 创建目录
     * @param path
     * @throws Exception
     */
    @Override
    public void mkdir(String path) throws Exception {
        MkdirRequest request = MkdirRequest.newBuilder().setPath(path).build();

        MkdirResponse response = namenode.mkdir(request);
        log.info("创建目录的响应：" + response.getStatus());
    }

    @Override
    public void shutdown() throws Exception {
        ShutdownRequest request = ShutdownRequest.newBuilder().setCode(1).build();
        namenode.shutdown(request);

    }

    @Override
    public Boolean upload(FileInfo fileInfo, ResponseCallback callback) throws Exception {
        if (!creatFile(fileInfo.getFilename())) {
            return false;
        }
        JSONArray datanodes = allocateDateNodes(fileInfo.getFilename(), fileInfo.getFileLength());
        for (int i = 0; i < datanodes.size(); i++) {
            Host host = getHost(datanodes.getJSONObject(i));

            if (!nioClient.sendFile(fileInfo, host, callback)) {
                host = reallocateDataNode(fileInfo, host.getId());
                nioClient.sendFile(fileInfo, host, null);
            }
        }
        return true;
    }

    @Override
    public Boolean retryUpload(FileInfo fileInfo, Host exclueHost) throws Exception {
        Host host = reallocateDataNode(fileInfo, exclueHost.getId());
        nioClient.sendFile(fileInfo, host, null);
        return true;
    }

    @Override
    public byte[] download(String filename) throws Exception {
        Host datanode = chooseDataNodeFromReplicas(filename, "");

        byte[] file = null;
        try {
            file = nioClient.readFile(datanode, filename, true);
        } catch (Exception e) {
            datanode = chooseDataNodeFromReplicas(filename, datanode.getId());
            try {
                file = nioClient.readFile(datanode, filename,false);
            } catch (Exception e2) {
                log.error("error:", e2);
            }
        }
        return file;
    }

    /**
     * 获取数据对应的机器
     * @param datanode
     * @return
     */
    private Host getHost(JSONObject datanode) {
        Host host = new Host();
        host.setHostname(datanode.getString("hostname"));
        host.setIp(datanode.getString("ip"));
        host.setNioPort(datanode.getInteger("nioPort"));
        return host;
    }

    /**
     * 发送请求到master节点创建文件
     * @param filename
     * @return
     */
    private Boolean creatFile(String filename) {
        CreateFileRequest request = CreateFileRequest.newBuilder().setFilename(filename).build();
        CreateFileResponse response = namenode.create(request);

        if (response.getStatus() == 1) {
            return true;
        }
        return false;
    }

    /**
     * 分配双副本对应的数据节点
     * @param filename
     * @param fileSize
     * @return
     */
    public JSONArray allocateDateNodes(String filename, long fileSize) {
        AllocateDataNodesRequest request = AllocateDataNodesRequest.newBuilder().setFilename(filename).setFileSize(fileSize).build();
        AllocateDataNodesResponse response = namenode.allocateDataNodes(request);
        return JSON.parseArray(response.getDatanodes());
    }

    /**
     * 分配双副本对应的数据节点
     * @param fileInfo
     * @param excludedHostId
     * @return
     */
    public Host reallocateDataNode(FileInfo fileInfo, String excludedHostId) {
        ReallocateDataNodeRequest request = ReallocateDataNodeRequest.newBuilder().setFilename(fileInfo.getFilename())
                .setFileSize(fileInfo.getFileLength())
                .setExcludedDataNodeId(excludedHostId)
                .build();
        ReallocateDataNodeResponse response = namenode.reallocateDataNode(request);
        return getHost(JSONObject.parseObject(response.getDatanode()));
    }

    /**
     * 获取文件的某个副本所在的机器
     * @param filename
     * @param excludedDataNodeId
     * @return
     * @throws Exception
     */
    private Host chooseDataNodeFromReplicas(String filename, String excludedDataNodeId) throws Exception {
        ChooseDataNodeFromReplicasRequest request = ChooseDataNodeFromReplicasRequest.newBuilder()
                .setFilename(filename)
                .setExcludedDataNodeId(excludedDataNodeId)
                .build();
        ChooseDataNodeFromReplicasResponse response = namenode.chooseDataNodeFromReplicas(request);
        return getHost(JSONObject.parseObject(response.getDatanode()));
    }
}
