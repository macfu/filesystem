package com.macfu.server.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * 客户端的一个NIOClient，负责跟数据节点进行网络通信
 */
@Slf4j
public class NioClient {

    public static final Integer READ_FILE = 2;

    /**
     * 读取文件
     * @param hostname
     * @param nioPort
     * @param filename
     * @return
     */
    public byte[] readFile(String hostname, int nioPort, String filename) {
        ByteBuffer fileLengthBuffer = null;
        Long fileLength = null;
        ByteBuffer fileBuffer = null;
        byte[] file = null;

        SocketChannel channel = null;
        Selector selector = null;

        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(hostname, nioPort));

            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_CONNECT);

            boolean reading = true;
            while (reading) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    // NIOServer允许进行连接的话
                    if (key.isConnectable()) {
                        channel = (SocketChannel) key.channel();

                        if (channel.isConnectionPending()) {
                            // 三次握手做完，tcp连接建立好了
                            channel.finishConnect();
                        }

                        /**
                         * 在这里，第一步，一旦建立连接，直接就是发送过去一个请求
                         * 意思就是说，你想要读取一个文件
                         * 其实你就应该先发送你这个请求要做的事情，比如用Integer类型来进行代表，4个字节，int数据
                         * 1：发送文件；2：读取文件
                         * 2+文件名的字节数+实际的文件名
                         * 客户端发送完请求之后，立马就是关注OP_READ事件，他要等着去读取人家发送过来的数据
                         * 一旦说读取完毕了文件，再次关注OP_WRITE，发送一个SUCCESS过去给人家
                         *
                         */
                        /**
                         * 服务器而言，先读取开头的四个字节，判断一下，你要干什么
                         * 如果是1：发送文件，就转入之前写的那套代码；如果是2：读取文件，那么就需要一套新逻辑
                         * 人家就需要解析出来你的文件名，转换为他的本地存储的路径，把文件读取出来，给你发送过来
                         * 一旦发送完毕了文件之后，就关注OP_READ时间，等待读取人家发送过来的结果
                         */
                        byte[] filenameBytes = filename.getBytes();
                        ByteBuffer readFileRequest = ByteBuffer.allocate(4 + 4 + filenameBytes.length);
                        readFileRequest.putInt(READ_FILE);
                        readFileRequest.putInt(filenameBytes.length);
                        readFileRequest.put(filenameBytes);
                        readFileRequest.flip();

                        channel.write(readFileRequest);
                        log.info("发送文件下载的请求过去......");
                        key.interestOps(SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        channel = (SocketChannel) key.channel();
                        if (fileLength == null) {
                            if (fileLengthBuffer == null) {
                                fileLengthBuffer = ByteBuffer.allocate(8);
                            }
                            channel.read(fileLengthBuffer);
                            if (!fileLengthBuffer.hasRemaining()) {
                                fileLengthBuffer.rewind();
                                fileLength = fileLengthBuffer.getLong();
                                log.info("从服务器返回数据中解析文件大小：" + fileLength);
                            }
                        }
                        if (fileLength != null) {
                            if (fileBuffer == null) {
                                fileBuffer = ByteBuffer.allocate(Integer.valueOf(String.valueOf(fileLength)));
                            }
                            int hasRead = channel.read(fileBuffer);
                            log.info("从服务器读取了" + hasRead + "bytes的数据出来到内存中");

                            if (!fileBuffer.hasRemaining()) {
                                fileBuffer.rewind();
                                file = fileBuffer.array();
                                log.info("最终获取到的文件的大小为：" + file.length + "bytes");
                                reading = false;
                            }
                        }
                    }
                }
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
