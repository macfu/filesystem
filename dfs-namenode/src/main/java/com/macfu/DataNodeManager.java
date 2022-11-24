package com.macfu;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责管理集群里的所有的datanode
 */
@Slf4j
public class DataNodeManager {

    private Map<String, DataNodeInfo> datanodes = new ConcurrentHashMap<String, DataNodeInfo>();

    private FSNamesystem fsNamesystem;

    public DataNodeManager() {
        new DataNodeAliveMonitor().start();
    }

    public void setFsNamesystem(FSNamesystem fsNamesystem) {
        this.fsNamesystem = fsNamesystem;
    }

    /**
     * datanode 进行注册
     * @param ip
     * @param hostname
     * @param port
     * @return
     */
    public Boolean register(String ip, String hostname, int port) {
        if (datanodes.containsKey(ip + "-" + hostname)) {
            log.error("注册失败，当前已经存在这个DataNode了......");
            return false;
        }

        DataNodeInfo dataNodeInfo = new DataNodeInfo(ip, hostname, port);
        datanodes.put(ip + "-" + hostname, dataNodeInfo);
        log.info("DataNode注册：ip=" + ip + ",hostname=" + hostname + ",nioport=" + port);
        return true;
    }

    public Boolean heartBeat(String ip, String hostname) {
        DataNodeInfo dataNodeInfo = datanodes.get(ip + "-" + hostname);
        if (dataNodeInfo == null) {
            log.info("心跳失败，需要重新注册......");
            return false;
        }
        dataNodeInfo.setLatestHeartBeatTime(System.currentTimeMillis());
        log.info("DataNode发送心跳:ip=" + ip + ",hostname=" + hostname);
        return true;
    }

    /**
     * 分配双副本对应的数据节点
     * @param fileSize
     * @return
     */
    public List<DataNodeInfo> allocateDataNodes(long fileSize) {
        synchronized (this) {
            // 按照已经分配的存储大小来排序
            List<DataNodeInfo> dataNodeInfoList = Lists.newArrayList();
            for (DataNodeInfo dataNodeInfo : datanodes.values()) {
                dataNodeInfoList.add(dataNodeInfo);
            }
            Collections.sort(dataNodeInfoList);

            List<DataNodeInfo> selectDataNodes = Lists.newArrayList();
            if (dataNodeInfoList.size() >= 2) {
                selectDataNodes.add(dataNodeInfoList.get(0));
                selectDataNodes.add(dataNodeInfoList.get(1));

                /**
                 * 这里的副本数量是可以动态配置的：
                 * 默认认为要上传的文件会被放到存储数据量最少的两个上面
                 * 此时更新要放数据的两个datanode节点的存储数据量大小
                 */
                dataNodeInfoList.get(0).setStoredDataSize(fileSize);
                dataNodeInfoList.get(1).setStoredDataSize(fileSize);
            }
            return selectDataNodes;
        }
    }

    /**
     * 分配双副本对应的数据节点
     * @param fileSize
     * @param excludeDataNodeId
     * @return
     */
    public DataNodeInfo reallocateDatNode(long fileSize, String excludeDataNodeId) {
        synchronized (this) {
            // 需要先把排除掉的那个数据节点的存储的数据量减少文件的大小
            DataNodeInfo excludeDataNode = datanodes.get(excludeDataNodeId);
            excludeDataNode.setStoredDataSize(-fileSize);

            List<DataNodeInfo> dataNodeInfoList = Lists.newArrayList();
            for (DataNodeInfo dataNodeInfo : datanodes.values()) {
                if (!excludeDataNode.equals(dataNodeInfo)) {
                    dataNodeInfoList.add(dataNodeInfo);
                }
            }
            Collections.sort(dataNodeInfoList);

            DataNodeInfo selectedDataNode = null;
            if (dataNodeInfoList.size() >= 1) {
                selectedDataNode = dataNodeInfoList.get(0);
                dataNodeInfoList.get(0).setStoredDataSize(fileSize);
            }
            return selectedDataNode;
        }
    }

    /**
     * 设置指定DataNode的存储数据大小
     * @param ip
     * @param hostname
     * @param storedDataSize
     */
    public void setStoredDataSize(String ip, String hostname, Long storedDataSize) {
        DataNodeInfo dataNodeInfo = datanodes.get(ip + "-" + hostname);
        dataNodeInfo.setStoredDataSize(storedDataSize);
    }

    /**
     * 获取DataNode信息
     * @param ip
     * @param hostname
     * @return
     */
    public DataNodeInfo getDataNode(String ip, String hostname) {
        return datanodes.get(ip + "-" + hostname);
    }

    /**
     * 创建死去节点副本的赋值任务
     * @param deadNodeInfo
     */
    public void createReplicateTask(DataNodeInfo deadNodeInfo) {
        synchronized (this) {
            List<String> files = fsNamesystem.getFilesByDataNode(deadNodeInfo.getIp(), deadNodeInfo.getHostname());
            for (String file : files) {
                String filename = file.split("_")[0];
                Long fileLength = Long.valueOf(file.split("_")[1]);
                // 获取这个复制任务的源头数据节点
                DataNodeInfo sourceDatNode = fsNamesystem.getReplicateSource(filename, deadNodeInfo);

                DataNodeInfo destDataNode = allocateReplicateDataNode(fileLength, sourceDatNode, deadNodeInfo);

                ReplicateTask replicateTask = new ReplicateTask(filename, fileLength, sourceDatNode, destDataNode);
                destDataNode.addReplicateTask(replicateTask);
                log.info("为目标数据节点生成一个副本复制任务：" + replicateTask);
            }
        }
    }

    /**
     * 分配用来复制副本的数据节点
     * @param fileSize
     * @param sourceDatanode
     * @param deadDataNode
     * @return
     */
    public DataNodeInfo allocateReplicateDataNode(long fileSize, DataNodeInfo sourceDatanode, DataNodeInfo deadDataNode) {
        synchronized (this) {
            List<DataNodeInfo> dataNodeList = Lists.newArrayList();
            for (DataNodeInfo dataNodeInfo : datanodes.values()) {
                if (!dataNodeInfo.equals(sourceDatanode) && !dataNodeInfo.equals(deadDataNode)) {
                    dataNodeList.add(dataNodeInfo);
                }
            }
            Collections.sort(dataNodeList);

            DataNodeInfo selectDataNode = null;
            if (CollectionUtils.isNotEmpty(dataNodeList)) {
                selectDataNode = dataNodeList.get(0);
                dataNodeList.get(0).setStoredDataSize(fileSize);
            }
            return selectDataNode;
        }
    }

    /**
     * 为重平衡去创建副本复制的任务
     */
    public void createRebalanceTasks() {
        synchronized (this) {
            // 计算集群节点存数数据的平均值
            long totalStoredDataSize = 0;
            for (DataNodeInfo dataNodeInfo : datanodes.values()) {
                totalStoredDataSize += dataNodeInfo.getStoredDataSize();
            }
            long averageStoredDataSize = totalStoredDataSize / datanodes.size();
            // 将集群中的节点区分为两类，迁出节点和迁入节点
            List<DataNodeInfo> sourceDataNodes = Lists.newArrayList();
            List<DataNodeInfo> destDataNodes = Lists.newArrayList();

            for (DataNodeInfo dataNodeInfo : datanodes.values()) {
                if (dataNodeInfo.getStoredDataSize() > averageStoredDataSize) {
                    sourceDataNodes.add(dataNodeInfo);
                }
                if (dataNodeInfo.getStoredDataSize() < averageStoredDataSize) {
                    destDataNodes.add(dataNodeInfo);
                }
            }

            /**
             * 为迁入节点生成复制任务，为迁出节点生成删除任务
             * 在这里生成的删除任务同一放到24小时之后延迟调度执行
             */
            List<RemoveReplicaTask> removeReplicaTasks = Lists.newArrayList();

            for (DataNodeInfo sourceDataNode : sourceDataNodes) {
                long toRemoveDataSize = sourceDataNode.getStoredDataSize() - averageStoredDataSize;
                for (DataNodeInfo destDataNode : destDataNodes) {
                    if (destDataNode.getStoredDataSize() + toRemoveDataSize <= averageStoredDataSize) {
                        createRebalanceTasks(sourceDataNode, destDataNode, removeReplicaTasks, toRemoveDataSize);
                        break;
                    } else if (destDataNode.getStoredDataSize() < averageStoredDataSize) {
                        long maxRemoveDataSize = averageStoredDataSize - destDataNode.getStoredDataSize();
                        long removedDataSize = createRebalanceTasks(sourceDataNode, destDataNode, removeReplicaTasks, maxRemoveDataSize);
                        toRemoveDataSize -= removedDataSize;
                    }
                }
                new DelayRemoveReplicaThread(removeReplicaTasks).start();
            }
        }
    }

    private long createRebalanceTasks(DataNodeInfo sourceDataNode, DataNodeInfo destDataNode,
                                      List<RemoveReplicaTask> removeReplicaTasks, long maxRemoveDataSize) {
        List<String> files = fsNamesystem.getFilesByDataNode(sourceDataNode.getIp(), sourceDataNode.getHostname());

        /**
         * 遍历文件，不停的为每个文件生成一个复制任务，知道准备迁移的文件
         * 超过了待迁移总数据量的大小为止
         */
        long removedDataSize = 0;
        for (String file : files) {
            String filename = file.split("_")[0];
            long fileLength = Long.valueOf(file.split("_")[1]);
            if (removedDataSize + fileLength >= maxRemoveDataSize) {
                break;
            }
            // 为这个文件生成复制任务
            ReplicateTask replicateTask = new ReplicateTask(filename, fileLength, sourceDataNode, destDataNode);
            destDataNode.addReplicateTask(replicateTask);
            destDataNode.setStoredDataSize(fileLength);

            // 为这个文件生成删除任务
            sourceDataNode.setStoredDataSize(-fileLength);
            fsNamesystem.removeReplicaFromDataNode(sourceDataNode.getId(), file);
            RemoveReplicaTask removeReplicaTask = new RemoveReplicaTask(filename, sourceDataNode);
            removeReplicaTasks.add(removeReplicaTask);
            removedDataSize += fileLength;
        }
        return removedDataSize;
    }

    /**
     * 延迟删除副本的线程
     */
    class DelayRemoveReplicaThread extends Thread {

        private List<RemoveReplicaTask> removeReplicaTasks;

        public DelayRemoveReplicaThread(List<RemoveReplicaTask> removeReplicaTasks) {
            this.removeReplicaTasks = removeReplicaTasks;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            while (true) {
                try {
                    long now = System.currentTimeMillis();
                    if (now - start > 24 * 60 * 60 * 1000) {
                        for (RemoveReplicaTask removeReplicaTask : removeReplicaTasks) {
                            removeReplicaTask.getDataNodeInfo().addRemoveReplicaTask(removeReplicaTask);
                        }
                        break;
                    }
                    Thread.sleep(60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取DataNode信息
     * @param id
     * @return
     */
    public DataNodeInfo getDataNode(String id) {
        return datanodes.get(id);
    }

    /**
     * datanode是否存活的监控线程
     */
    class DataNodeAliveMonitor extends Thread {

        @Override
        public void run() {
            try {
                while (true) {
                    List<DataNodeInfo> toRemoveDataNodes = Lists.newArrayList();

                    Iterator<DataNodeInfo> dataNodesIterator = datanodes.values().iterator();
                    DataNodeInfo dataNodeInfo = null;
                    while (dataNodesIterator.hasNext()) {
                        dataNodeInfo = dataNodesIterator.next();
                        if (System.currentTimeMillis() - dataNodeInfo.getLatestHeartBeatTime() > 1 * 60 * 1000) {
                            toRemoveDataNodes.add(dataNodeInfo);
                        }
                    }
                    if (!toRemoveDataNodes.isEmpty()) {
                        for (DataNodeInfo toRemoveDataNode : toRemoveDataNodes) {
                            log.info("数据节点【" + toRemoveDataNode + "】宕机，需要进行副本复制......");

                            createReplicateTask(toRemoveDataNode);

                            datanodes.remove(toRemoveDataNode.getId());
                            log.info("从内存数据结构中删除掉这个节点:" +  toRemoveDataNode);
                            fsNamesystem.removedDeadDataNode(toRemoveDataNode);
                        }
                    }
                    Thread.sleep(30 * 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
