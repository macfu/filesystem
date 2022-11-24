package com.macfu.client;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * 客户端的一个NIOClient，负责跟数据节点进行网络通信
 */
public class NioClient {

    private NetworkManager networkManager;

    public NioClient() {
        this.networkManager = new NetworkManager();
    }

    /**
     * 发送一个文件过去
     * @param fileInfo
     * @param host
     * @param responseCallback
     * @return
     */
    public Boolean sendFile(FileInfo fileInfo, Host host, ResponseCallback responseCallback) {
        /**
         * 一般来说，这里应该先根据hostname检查一下，跟对方机器的连接是否已经建立
         * 如果还咩有建立好，那么这里就直接开始建立连接
         * 建立好连接之后，就应该吧连接给缓存起来，以备下次来进行使用
         */
        if (!networkManager.maybeConnect(host.getHostname(), host.getNioPort())) {
            return false;
        }

        NetWorkRequest request = createSendFileRequest(fileInfo, host, responseCallback);
        networkManager.sendRequest(request);

        return true;
    }

    public byte[] readFile(Host host, String filename, Boolean retry) throws Exception {
        if (!networkManager.maybeConnect(host.getHostname(), host.getNioPort())) {
            if (retry) {
                throw new Exception();
            }
        }
        NetWorkRequest request = createReadFileRequest(host, filename, null);
        networkManager.sendRequest(request);

        NetworkResponse response = networkManager.waitResponse(request.getId());
        if (response.getError()) {
            if (retry) {
                throw new Exception();
            }
        }
        return response.getBuffer().array();
    }

    /**
     * 构建一个发送文件的网络请求
     * @param fileInfo
     * @param host
     * @param callback
     * @return
     */
    private NetWorkRequest createSendFileRequest(FileInfo fileInfo, Host host, ResponseCallback callback) {
        NetWorkRequest request = new NetWorkRequest();

        ByteBuffer buf = ByteBuffer.allocate(NetWorkRequest.REQUEST_TYPE
                + NetWorkRequest.FILENAME_LENGTH
                + fileInfo.getFilename().getBytes().length
                + NetWorkRequest.FILE_LENGTH
                + (int) fileInfo.getFileLength());
        buf.putInt(NetWorkRequest.REQUEST_SEND_FILE);
        buf.putInt(fileInfo.getFilename().getBytes().length);
        buf.put(fileInfo.getFilename().getBytes());
        buf.putLong(fileInfo.getFileLength());
        buf.put(fileInfo.getFile());
        buf.rewind();

        request.setId(UUID.randomUUID().toString());
        request.setHostname(host.getHostname());
        request.setRequestType(NetWorkRequest.REQUEST_SEND_FILE);
        request.setIp(host.getIp());
        request.setNioPort(host.getNioPort());
        request.setBuffer(buf);
        request.setNeedResponse(false);
        request.setCallback(callback);
        return request;
    }

    /**
     * 构建一个读取文件的网络请求
     * @param host
     * @param filename
     * @param callback
     * @return
     */
    private NetWorkRequest createReadFileRequest(Host host, String filename, ResponseCallback callback) {
        NetWorkRequest request = new NetWorkRequest();

        byte[] filenameBytes = filename.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(
                NetWorkRequest.REQUEST_TYPE
                + NetWorkRequest.FILE_LENGTH
                + filenameBytes.length);
        buffer.putInt(NetWorkRequest.REQUEST_READ_FILE);
        buffer.putInt(filenameBytes.length);
        buffer.put(filenameBytes);
        buffer.rewind();

        request.setId(UUID.randomUUID().toString());
        request.setHostname(host.getHostname());
        request.setRequestType(NetWorkRequest.REQUEST_READ_FILE);
        request.setId(host.getIp());
        request.setNioPort(host.getNioPort());
        request.setBuffer(buffer);
        request.setNeedResponse(true);
        request.setCallback(callback);
        return request;
    }
}
