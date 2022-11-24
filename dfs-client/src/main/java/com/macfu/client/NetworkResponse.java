package com.macfu.client;

import java.nio.ByteBuffer;

/**
 * 响应结果
 */
public class NetworkResponse {

    private String requestId;
    private String hostname;
    private String ip;
    private ByteBuffer lengthBuffer;
    private ByteBuffer buffer;
    private Boolean error;
    private Boolean finished;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public ByteBuffer getLengthBuffer() {
        return lengthBuffer;
    }

    public void setLengthBuffer(ByteBuffer lengthBuffer) {
        this.lengthBuffer = lengthBuffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public Boolean getError() {
        return error;
    }

    public void setError(Boolean error) {
        this.error = error;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
