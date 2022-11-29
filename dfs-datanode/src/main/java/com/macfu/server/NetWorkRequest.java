package com.macfu.server;

import com.macfu.server.util.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 网络请求
 */
@Slf4j
public class NetWorkRequest {

    public static final Integer REQUEST_SEND_FILE = 1;
    public static final Integer REQUEST_READ_FILE = 2;

    // processor标识
    private Integer processorId;
    // 该请求是哪个客户端 发送过来的
    private String client;
    // 本次网络请求对应的连接
    private SelectionKey key;
    // 本次网络请求对应的连接
    private SocketChannel channel;

    private CachedRequest cachedRequest = new CachedRequest();
    private ByteBuffer cachedRequestTypeBuffer;
    private ByteBuffer cachedFilenameLengthBuffer;
    private ByteBuffer cachedFilenameBuffer;
    private ByteBuffer cachedFileLengthBuffer;
    private ByteBuffer cachedFileBuffer;

    public NetWorkRequest() {
        log.info("请求初始化：" + DataNodeConfig.DATA_DIR);
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    /**
     * 从网路请求中读取与解析出来一个请求
     */
    public void read() {
        /**
         * 假如说你这个一次读取的数据里包含李多个文件的话
         * 这个时候我们会先读取文件名，然后根据文件的大小去读取这么多的数据
         * 需要先提取出来这次请求是什么类型：1发送文件；2读取文件
         */
        try {
            log.info("开始读取请求：" + DataNodeConfig.DATA_DIR);
            Integer requestType = null;
            if (cachedRequest.requestType != null) {
                requestType = cachedRequest.requestType;
            } else {
                // 此时channel的positio肯定也变为了4
                requestType = getRequestType(channel);
            }
            if (requestType == null) {
                return;
            }
            log.info("从请求中解析出来的请求类型：" + requestType);
            /**
             * 执行拆包操作，就是说一次请求，本来是包含了：requestType + filenamelength + filename[+imageLength + image]
             * 这次op_read事件，就读取到了requestType的4个字节中的两个字节，剩余的数据就被放在下一个op_read事件中了
             */
            if (REQUEST_SEND_FILE.equals(requestType)) {
                handleSendFileRequest(channel, key);
            } else if (REQUEST_READ_FILE.equals(requestType)) {
                handleReadFileRequest(channel, key);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取本次请求的类型
     * @return
     * @throws Exception
     */
    public Integer getRequestType(SocketChannel channel) throws Exception {
        Integer requestType = null;
        if (cachedRequest.requestType != null) {
            return cachedRequest.requestType;
        }

        ByteBuffer requestTypeBuffer = null;
        if (cachedRequestTypeBuffer != null) {
            requestTypeBuffer = cachedRequestTypeBuffer;
        } else {
            requestTypeBuffer = ByteBuffer.allocate(4);
        }

        // 此时requestTypeByteBuffer， position跟limit都是4，remaining是0
        channel.read(requestTypeBuffer);
        if (!requestTypeBuffer.hasRemaining()) {
            // 已经读取出来4个字节，可以提取出来requestType了
            // 将position变为0，limit还是维持着4
            requestTypeBuffer.rewind();
            requestType = requestTypeBuffer.getInt();
            cachedRequest.requestType = requestType;
        } else {
            cachedRequestTypeBuffer = requestTypeBuffer;
        }
        return requestType;
    }

    /**
     * 发送文件
     * @param channel
     * @param key
     * @throws Exception
     */
    public void handleSendFileRequest(SocketChannel channel, SelectionKey key) throws Exception {
        Filename filename = getFilename(channel);
        log.info("从网络请求中解析出来文件名：" + filename);
        if (filename == null) {
            return;
        }
        Long fileLength = getFileLength(channel);
        log.info("从网络请求中解析出来文件大小：" + fileLength);
        if (fileLength == null) {
            return;
        }

        // 循环不断的从channel里读取数据，并写入磁盘文件
        ByteBuffer fileBuffer = null;
        if (cachedFileBuffer != null) {
            fileBuffer = cachedFileBuffer;
        } else {
            fileBuffer = ByteBuffer.allocate(Integer.valueOf(String.valueOf(fileLength)));
        }
        channel.read(fileBuffer);
        if (!fileBuffer.hasRemaining()) {
            fileBuffer.rewind();
            cachedRequest.file = fileBuffer;
            cachedRequest.hasCompleteRead = true;
            log.info("本次文件上传读取请求完毕......");
        } else {
            cachedFileBuffer = fileBuffer;
            log.info("本次文件上传出现拆包问题，缓存起来，下次继续读取");
            return;
        }
    }

    /**
     * 获取文件名同时转换为本地磁盘目录中的绝对路径
     * @param channel
     * @return
     * @throws Exception
     */
    public Filename getFilename(SocketChannel channel) throws Exception {
        Filename filename = new Filename();
        if (cachedRequest.filename != null) {
            return cachedRequest.filename;
        } else {
            String relativeFilename = getRelativeFilename(channel);
            if (relativeFilename == null) {
                return null;
            }

            String absoluteFilename = getAbsoluteFilename(relativeFilename);
            filename.relativeFilename = relativeFilename;
            filename.absoluteFilename = absoluteFilename;

            cachedRequest.filename = filename;
        }
        return filename;
    }

    /**
     * 获取相对路径的文件名
     * @param socketChannel
     * @return
     * @throws Exception
     */
    private String getRelativeFilename(SocketChannel socketChannel) throws Exception {
        Integer filenameLength = null;
        String filename = null;
        // 读取文件名的大小
        if (cachedRequest.filenameLength == null) {
            ByteBuffer filenameLengthBuffer = null;
            if (cachedFilenameBuffer != null) {
                filenameLengthBuffer = cachedFilenameLengthBuffer;
            } else {
                filenameLengthBuffer = ByteBuffer.allocate(4);
            }

            channel.read(filenameLengthBuffer);
            if (!filenameLengthBuffer.hasRemaining()) {
                filenameLengthBuffer.rewind();
                filenameLength = filenameLengthBuffer.getInt();
                cachedRequest.filenameLength = filenameLength;
            } else {
                cachedFilenameBuffer = filenameLengthBuffer;
                return null;
            }
        }

        // 读取文件名
        ByteBuffer filenameBuffer = null;
        if (cachedFilenameBuffer != null) {
            filenameBuffer = cachedFilenameBuffer;
        } else {
            filenameBuffer = ByteBuffer.allocate(filenameLength);
        }
        channel.read(filenameBuffer);
        if (!filenameBuffer.hasRemaining()) {
            filenameBuffer.rewind();
            filename = new String(filenameBuffer.array());
        } else {
            cachedFilenameBuffer = filenameBuffer;
        }
        return filename;
    }

    /**
     * 获取文件在本地磁盘上的绝对路径名
     * @param relativeFilename
     * @return
     * @throws Exception
     */
    public String getAbsoluteFilename(String relativeFilename) throws Exception {
        String[] relativeFilenameSplited = relativeFilename.split("/");
        String dirPath = DataNodeConfig.DATA_DIR;
        log.info("dirpath：" + dirPath);

        for (int i = 0; i < relativeFilenameSplited.length - 1; i++) {
            if (i == 0) {
                continue;
            }
            dirPath += "\\" + relativeFilenameSplited[i];
        }

        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String absoluteFilename = dirPath + "\\" + relativeFilenameSplited[relativeFilenameSplited.length - 1];
        log.info("absoluteFilename:" + absoluteFilename);
        return absoluteFilename;
    }

    /**
     * 从网络请求中读取文件大小
     * @param channel
     * @return
     * @throws Exception
     */
    private Long getFileLength(SocketChannel channel) throws Exception {
        Long fileLength = null;
        if (cachedRequest.filenameLength != null) {
            return cachedRequest.fileLength;
        } else {
            ByteBuffer fileLengthBuffer = null;
            if (cachedFileLengthBuffer != null) {
                fileLengthBuffer = cachedFileLengthBuffer;
            } else {
                fileLengthBuffer = ByteBuffer.allocate(8);
            }
            channel.read(fileLengthBuffer);
            if (!fileLengthBuffer.hasRemaining()) {
                fileLengthBuffer.rewind();
                fileLength = fileLengthBuffer.getLong();
                cachedRequest.fileLength = fileLength;
            } else {
                cachedFileLengthBuffer = fileLengthBuffer;
            }
        }
        return fileLength;
    }

    /**
     * 读取文件
     */
    public void handleReadFileRequest(SocketChannel channel, SelectionKey key) throws Exception {
        Filename filename = getFilename(channel);
        log.info("从网络请求中解析出来文件名：" + filename);
        if (filename == null) {
            return;
        }
        cachedRequest.hasCompleteRead = true;
    }

    /**
     * 文件名
     */
    class Filename {
        // 相对路径名
        String relativeFilename;
        // 绝对路径名
        String absoluteFilename;

        @Override
        public String toString() {
            return "Filename{" +
                    "relativeFilename='" + relativeFilename + '\'' +
                    ", absoluteFilename='" + absoluteFilename + '\'' +
                    '}';
        }
    }

    /**
     * 缓存文件
     */
    class CachedRequest {
        Integer requestType;
        Filename filename;
        Integer filenameLength;
        Long fileLength;
        ByteBuffer file;
        Boolean hasCompleteRead = false;

        public Integer getRequestType() {
            return requestType;
        }

        public void setRequestType(Integer requestType) {
            this.requestType = requestType;
        }

        public Filename getFilename() {
            return filename;
        }

        public void setFilename(Filename filename) {
            this.filename = filename;
        }

        public Integer getFilenameLength() {
            return filenameLength;
        }

        public void setFilenameLength(Integer filenameLength) {
            this.filenameLength = filenameLength;
        }

        public Long getFileLength() {
            return fileLength;
        }

        public void setFileLength(Long fileLength) {
            this.fileLength = fileLength;
        }

        public ByteBuffer getFile() {
            return file;
        }

        public void setFile(ByteBuffer file) {
            this.file = file;
        }

        public Boolean getHasCompleteRead() {
            return hasCompleteRead;
        }

        public void setHasCompleteRead(Boolean hasCompleteRead) {
            this.hasCompleteRead = hasCompleteRead;
        }
    }
}
