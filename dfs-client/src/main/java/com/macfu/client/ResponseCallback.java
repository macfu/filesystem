package com.macfu.client;

/**
 * 响应回调函数接口
 */
public interface ResponseCallback {

    /**
     * 处理响应结果
     * @param response
     */
    void process(NetworkResponse response);
}
