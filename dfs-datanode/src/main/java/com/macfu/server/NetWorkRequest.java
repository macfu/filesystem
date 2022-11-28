package com.macfu.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 网络请求
 */
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
}
