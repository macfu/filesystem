package com.macfu;

/**
 * 生成editlog内容的工厂类
 */
public class EditLogFactory {

    public static String mkdir(String path) {
        return "{'OP':'MKDIR','PAHT':'" + path + "'}";
    }

    public static String create(String path) {
        return "{'OP':'CREATE','PAHT':'" + path + "'}";
    }
}
