package com.macfu;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * 负责fsimage文件上传的server
 */
@Slf4j
public class FSImageUploadServer extends Thread{

    private Selector selector;

    public FSImageUploadServer() {
        this.init();
    }

    private void init() {
        ServerSocketChannel serverSocketChannel = null;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(9000), 100);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        log.info("FSImageUploadServer启动，监听端口：9000");
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();
                    try {
                        handleRequest(next);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     * 请求处理分发
     * @param key
     * @throws IOException
     * @throws ClosedChannelException
     */
    private void handleRequest(SelectionKey key) throws IOException, ClosedChannelException {
        if (key.isAcceptable()) {
            handleConnectionRequest(key);
        } else if (key.isReadable()) {
            handleReadableRequest(key);
        } else if (key.isWritable()) {
            handleWriteableRequest(key);
        }
    }

    /**s
     * 处理backupnode连接请求
     */
    private void handleConnectionRequest(SelectionKey key) throws IOException {

        SocketChannel channel = null;
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            channel = serverSocketChannel.accept();
            if (channel != null) {
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (channel != null) {
                channel.close();
            }
        }

    }

    /**
     * 处理发送fsimage文件的请求
     * @param key
     * @throws IOException
     */
    private void handleReadableRequest(SelectionKey key) throws IOException {
        SocketChannel channel = null;
        try {
            String fsimageFilePath = "F:\\development\\editslog\\fsimage.meta";

            RandomAccessFile fsimageRAF = null;
            FileOutputStream fsimageOut = null;
            FileChannel fsimageFileChannle = null;

            try {
                channel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                int total = 0;
                int count = -1;

                if ((count = channel.read(buffer)) > 0) {
                    File file = new File(fsimageFilePath);
                    if (file.exists()) {
                        file.delete();
                    }

                    fsimageRAF = new RandomAccessFile(fsimageFilePath, "rw");
                    fsimageOut = new FileOutputStream(fsimageRAF.getFD());
                    fsimageFileChannle = fsimageOut.getChannel();
                    total += count;
                    buffer.flip();
                    fsimageFileChannle.write(buffer);
                    buffer.clear();
                } else {
                    channel.close();
                }

                while ((count = channel.read(buffer)) > 0) {
                    total += count;
                    buffer.flip();
                    fsimageFileChannle.write(buffer);
                    buffer.clear();
                }

                if (total > 0) {
                    log.info("接受fsimage文件以及写入磁盘完毕......");
                    fsimageFileChannle.force(false);
                    channel.register(selector, SelectionKey.OP_WRITE);
                }
            } finally {
                if (fsimageOut != null) {
                    fsimageOut.close();
                }
                if (fsimageRAF != null) {
                    fsimageRAF.close();
                }
                if (fsimageFileChannle != null) {
                    fsimageFileChannle.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (channel != null) {
                channel.close();
            }
        }
    }

    /**
     * 处理返回响应给backupnode
     *
     * @param key
     * @throws IOException
     */
    private void handleWriteableRequest(SelectionKey key) throws IOException {
        SocketChannel channel = null;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put("SUCCESS".getBytes());
            buffer.flip();

            channel = (SocketChannel) key.channel();
            channel.write(buffer);
            log.info("fsimage上传完毕，返回响应success给backupnode......");
            channel.register(selector, SelectionKey.OP_READ);

        } catch (Exception e) {
            e.printStackTrace();
            if (channel != null) {
                channel.close();
            }
        }
    }


































}
