package com.macfu;

/**
 * NameNode核心启动类
 */
public class NameNode {

    /**
     * 负责管理元数据的核心组件：管理的是一些文件目录树，支持权限设置
     */
    private FSNamesystem namesystem;
    /**
     * 负责管理集群中所有的datanode得组件
     */
    private DataNodeManager dataNodeManager;

    private NameNodeR
}
