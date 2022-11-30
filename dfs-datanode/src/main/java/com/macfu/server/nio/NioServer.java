package com.macfu.server.nio;

import com.google.common.collect.Lists;
import com.macfu.server.IOThread;
import com.macfu.server.NameNodeRpcClient;
import com.macfu.server.NetWorkResponseQueue;
import com.macfu.server.util.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 数据节点的NIOServer
 */
@Slf4j
public class NioServer extends Thread {

    public static final Integer PROCESSOR_THREAD_NUM = 10;
    public static final Integer IO_THREAD_NUM = 10;

    // Nio的selector，负责多路复用监听多个连接的请求
    private Selector selector;
    // 负责解析请求和发送响应的processor线程
    private List<NioProcessor> processorList = Lists.newArrayList();
    // 与NameNode进行通信的客户端
    private NameNodeRpcClient namenode;

    /**
     * NIOServer的初始化，监听端口，队列初始化，线程初始化
     * @param nameNodeRpcClient
     */
    public NioServer(NameNodeRpcClient nameNodeRpcClient) {
        this.namenode = nameNodeRpcClient;
    }

    public void init() {
        // 这个地方的代码就是让NIOServer去监听指定的端口号
        try {
            // 需要用一个Selector多路复用监听多个连接的事件
            // 同步非阻塞的效果，也可是实现单个线程支撑N个连接的高并发架构
            selector = Selector.open();

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            // 必须将channel给设置为非阻塞的
            serverSocketChannel.configureBlocking(false);
            // 因为只要这样在底层Selector在多路复用监听的时候，才不会阻塞在某个channel上
            serverSocketChannel.socket().bind(new InetSocketAddress(DataNodeConfig.NIO_PORT));
            // 其实就是将ServerSocketChannel注册到Selector上，关注的事件，就是OP_ACCEPT
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            log.info("NIOServer已经启动，开始监听端口：" + DataNodeConfig.NIO_PORT);

            // 启动固定数量的Processor线程
            NetWorkResponseQueue responseQueues = NetWorkResponseQueue.get();

            for (int i = 0; i < PROCESSOR_THREAD_NUM; i++) {
                NioProcessor processor = new NioProcessor(i);
                processorList.add(processor);
                processor.start();

                responseQueues.initResponseQueue(i);
            }

            //启动固定数量的Processor线程
            for (int i = 0; i < IO_THREAD_NUM; i++) {
                new IOThread(namenode).start();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        /**
         * 无线循环，等待IO多路复用方式监听请求
         */
        while (true) {
            try {
                // 同步非阻塞
                selector.select();

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();

                    if (next.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) next.channel();
                        // 跟每个客户端建立连接
                        SocketChannel accept = channel.accept();
                        if (accept != null) {
                            channel.configureBlocking(false);

                            /**
                             * 如果一旦跟某个客户端建立了连接之后，就需要将这个客户端均匀分给后续的process线程
                             */
                            Integer processorIndex = new Random().nextInt(PROCESSOR_THREAD_NUM);
                            NioProcessor processor = processorList.get(processorIndex);
                            processor.addChannel(accept);

                        }
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

     
}
