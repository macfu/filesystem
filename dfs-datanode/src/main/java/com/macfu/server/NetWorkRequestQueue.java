package com.macfu.server;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 网络请求队列
 */
public class NetWorkRequestQueue {

    public static volatile NetWorkRequestQueue instance = null;

    public static NetWorkRequestQueue get() {
        if (instance == null) {
            synchronized (NetWorkRequestQueue.class) {
                if (instance == null) {
                    instance = new NetWorkRequestQueue();
                }
            }
        }
        return instance;
    }

    private ConcurrentLinkedQueue<NetWorkRequest> requestQueue = new ConcurrentLinkedQueue<>();

    public void offer(NetWorkRequest netWorkRequest) {
        requestQueue.offer(netWorkRequest);
    }

    public NetWorkRequest poll() {
        return requestQueue.poll();
    }
}
