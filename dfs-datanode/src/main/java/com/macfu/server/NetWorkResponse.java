package com.macfu.server;

import java.nio.ByteBuffer;

/**
 * 网络响应
 */
public class NetWorkResponse {

    private String client;
    private ByteBuffer buffer;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
