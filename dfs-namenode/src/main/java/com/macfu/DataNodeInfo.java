package com.macfu;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用来描述datanode的信息
 */
public class DataNodeInfo implements Comparable<DataNodeInfo> {
    // ip地址
    private String ip;
    // 机器名字
    private String hostname;
    // nio端口
    private int nioPort;
    // 最近一次心跳时间
    private long latestHeartBeatTime;
    // 已经存储的数据的大小
    private long storedDataSize;
    // 副本复制任务队列
    private ConcurrentLinkedQueue<ReplicateTask> replicateTaskQueue = new ConcurrentLinkedQueue<>();
    // 删除副本队列
    private ConcurrentLinkedQueue<RemoveReplicaTask> removeReplicaTaskQueue = new ConcurrentLinkedQueue<>();

    public DataNodeInfo(String ip, String hostname, int nioPort) {
        this.ip = ip;
        this.hostname = hostname;
        this.nioPort = nioPort;
        this.latestHeartBeatTime = System.currentTimeMillis();
        this.storedDataSize = 0L;
    }

    public void addReplicateTask(ReplicateTask replicateTask) {
        replicateTaskQueue.offer(replicateTask);
    }

    public ReplicateTask pollReplicateTask() {
        if (!replicateTaskQueue.isEmpty()) {
            return replicateTaskQueue.poll();
        }
        return null;
    }

    public void addRemoveReplicaTask(RemoveReplicaTask removeReplicaTask) {
        removeReplicaTaskQueue.offer(removeReplicaTask);
    }

    public RemoveReplicaTask pollRemoveReplicaTask() {
        if (!removeReplicaTaskQueue.isEmpty()) {
            return removeReplicaTaskQueue.poll();
        }
        return null;
    }

    public String getId() {
        return ip + "-" + hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getNioPort() {
        return nioPort;
    }

    public void setNioPort(int nioPort) {
        this.nioPort = nioPort;
    }

    public long getLatestHeartBeatTime() {
        return latestHeartBeatTime;
    }

    public void setLatestHeartBeatTime(long latestHeartBeatTime) {
        this.latestHeartBeatTime = latestHeartBeatTime;
    }

    public long getStoredDataSize() {
        return storedDataSize;
    }

    public void setStoredDataSize(long storedDataSize) {
        this.storedDataSize += storedDataSize;
    }

    @Override
    public int compareTo(DataNodeInfo o) {
        if (this.storedDataSize - o.getStoredDataSize() > 0) {
            return 1;
        } else if (this.storedDataSize - o.getStoredDataSize() < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "DataNodeInfo{" +
                "ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", nioPort=" + nioPort +
                ", latestHeartBeatTime=" + latestHeartBeatTime +
                ", storedDataSize=" + storedDataSize +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataNodeInfo other = (DataNodeInfo) obj;
        if (hostname == null) {
            if (other.hostname != null) {
                return false;
            }
        } else if (!hostname.equals(other.hostname)) {
            return false;
        }
        if (ip == null) {
            if (other.ip != null) {
                return false;
            }
        } else if (!ip.equals(other.ip)) {
            return false;
        }
        return true;
    }
}
