package com.macfu;

/**
 * 副本复制任务
 */
public class ReplicateTask {

    private String filename;
    private Long fileLength;
    private DataNodeInfo sourceDataNode;
    private DataNodeInfo destDataNode;

    public ReplicateTask(String filename, Long fileLength, DataNodeInfo sourceDataNode, DataNodeInfo destDataNode) {
        this.fileLength = fileLength;
        this.fileLength = fileLength;
        this.sourceDataNode = sourceDataNode;
        this.destDataNode = destDataNode;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileLength() {
        return fileLength;
    }

    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }

    public DataNodeInfo getSourceDataNode() {
        return sourceDataNode;
    }

    public void setSourceDataNode(DataNodeInfo sourceDataNode) {
        this.sourceDataNode = sourceDataNode;
    }

    public DataNodeInfo getDestDataNode() {
        return destDataNode;
    }

    public void setDestDataNode(DataNodeInfo destDataNode) {
        this.destDataNode = destDataNode;
    }

    @Override
    public String toString() {
        return "ReplicateTask{" +
                "filename='" + filename + '\'' +
                ", fileLength=" + fileLength +
                ", sourceDataNode=" + sourceDataNode +
                ", destDataNode=" + destDataNode +
                '}';
    }
}
