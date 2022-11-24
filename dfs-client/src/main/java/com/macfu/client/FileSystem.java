package com.macfu.client;

/**
 * 作为文件系统的接口
 */
public interface FileSystem {

    /**
     * 创建目录
     * @param path
     * @throws Exception
     */
    void mkdir(String path) throws Exception;

    /**
     * 优雅关闭
     * @throws Exception
     */
    void shutdown() throws Exception;

    /**
     * 上传文件
     * @param fileInfo
     * @param callback
     * @return
     * @throws Exception
     */
    Boolean upload(FileInfo fileInfo, ResponseCallback callback) throws Exception;

    /**
     * 重试上传文件
     * @param fileInfo
     * @param exclueHost
     * @return
     * @throws Exception
     */
    Boolean retryUpload(FileInfo fileInfo, Host exclueHost) throws Exception;

    /**
     * 下载文件
     * @param filename
     * @return
     * @throws Exception
     */
    byte[] download(String filename) throws Exception;
}
