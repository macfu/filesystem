package com.macfu.server.nio;

import com.google.common.collect.Maps;
import com.macfu.server.NetWorkRequest;
import com.macfu.server.NetWorkRequestQueue;
import com.macfu.server.NetWorkResponse;
import com.macfu.server.NetWorkResponseQueue;
import com.macfu.server.util.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 负责解析请求以及发送响应的线程
 */
@Slf4j
public class NioProcessor extends Thread {

    // 多路复用监听时的最大阻塞时间
    public static final Long POLL_BLOCK_MAX_TIME = 1000L;
    // processor标识
    private Integer processorId;
    // 等待注册网络连接的队列
    private ConcurrentLinkedQueue<SocketChannel> channelQueue = new ConcurrentLinkedQueue<>();
    // 每个processor私有的selector多路复用器
    private Selector selector;
    // 没读取完的请求缓存在这里
    private Map<String, NetWorkRequest> cachedRequests = Maps.newHashMap();
    // 暂存起来的响应缓存在这里
    private Map<String, NetWorkResponse> cachedResponses = Maps.newHashMap();
    // 这个processor负责维护的所有客户端的SelectionKey
    private Map<String, SelectionKey> cachedKeys = Maps.newHashMap();

    public NioProcessor(Integer processorId) {
        try {
            this.processorId = processorId;
            this.selector = Selector.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Integer getProcessorId() {
        return processorId;
    }

    public void setProcessorId(Integer processorId) {
        this.processorId = processorId;
    }

    public void addChannel(SocketChannel channel) {
        channelQueue.offer(channel);
        selector.wakeup();
    }

    /**
     * processor线程的核心逻辑
     */
    @Override
    public void run() {
        while (true) {
            try {
                // 注册排队等待的连接
                registerQueueClients();
                // 处理排队中的响应
                cachedQueueResponse();
                // 以阻塞的方式感知连接中的请求
                poll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将队列中等待注册的连接注册到selector上
     */
    private void registerQueueClients() {
        SocketChannel channel = null;
        while ((channel = channelQueue.poll()) != null) {
            try {
                channel.register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 暂存队列中的响应
     */
    private void cachedQueueResponse() {
        NetWorkResponseQueue responseQueues = NetWorkResponseQueue.get();
        NetWorkResponse response = null;

        while ((response = responseQueues.poll(processorId)) != null) {
            String client = response.getClient();
            cachedResponses.put(client, response);
            cachedKeys.get(client).interestOps(SelectionKey.OP_WRITE);
        }
    }

    /**
     * 以多路复用的方式来监听各个连接的请求
     */
    private void poll() {
        try {
            int keys = selector.select(POLL_BLOCK_MAX_TIME);
            if (keys > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    try {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        SocketChannel channel = (SocketChannel) key.channel();
                        String client = channel.getRemoteAddress().toString();

                        // 如果接受到了某个客户端的请求
                        if (key.isReadable()) {
                            log.info("准备读取请求:" + DataNodeConfig.DATA_DIR);
                            NetWorkRequest request = null;
                            if (cachedRequests.get(client) != null) {
                                request = cachedRequests.get(client);
                            } else {
                                request = new NetWorkRequest();
                                request.setChannel(channel);
                                request.setKey(key);
                            }
                            request.read();
                            log.info("读取请求完毕之后：" + DataNodeConfig.DATA_DIR);

                            if (request.hasCompletedRead()) {
                                request.setProcessorId(processorId);
                                request.setClient(client);

                                NetWorkRequestQueue requestQueue = NetWorkRequestQueue.get();
                                requestQueue.offer(request);

                                cachedKeys.put(client, key);
                                cachedRequests.remove(client);

                                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                                // 必须是的等到一个客户端的一个请求被处理完毕之后，才会允许读取下一个请求
                            } else {
                                cachedRequests.put(client, request);
                            }
                        } else if (key.isWritable()) {
                            NetWorkResponse response = cachedResponses.get(client);
                            channel.write(response.getBuffer());

                            cachedResponses.remove(client);
                            cachedKeys.remove(client);

                            key.interestOps(SelectionKey.OP_READ);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
