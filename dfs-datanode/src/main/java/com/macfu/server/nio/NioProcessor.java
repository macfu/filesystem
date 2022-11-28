package com.macfu.server.nio;

import com.google.common.collect.Maps;
import com.macfu.server.NetWorkRequest;
import com.macfu.server.NetWorkResponse;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 负责解析请求以及发送响应的线程
 */
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
}
