package com.macfu.server;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetWorkResponseQueues {

    public static volatile NetWorkResponseQueues instance = null;

    public static NetWorkResponseQueues get() {
        if (instance == null) {
            synchronized (NetWorkResponseQueues.class) {
                if (instance == null) {
                    instance = new NetWorkResponseQueues();
                }
            }
        }
        return instance;
    }

    private Map<Integer, ConcurrentLinkedQueue<NetWorkResponse>> responseQueues = Maps.newHashMap();

    public void initResponseQueue(Integer processorId) {
        ConcurrentLinkedQueue<NetWorkResponse> responseQueue = new ConcurrentLinkedQueue<>();
        responseQueues.put(processorId, responseQueue);
    }

    public void offer(Integer processId, NetWorkResponse response) {
        responseQueues.get(processId);
    }

    public NetWorkResponse poll(Integer processId) {
        return responseQueues.get(processId).poll();
    }
}
