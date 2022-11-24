package com.macfu;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * 负责管理内存中的文件目录树的核心组件
 */
@Slf4j
public class FSDirectory {

    // 内存中的文件目录树
    private INode dirTree;

    public INode getDirTree() {
        return dirTree;
    }

    public void setDirTree(INode dirTree) {
        this.dirTree = dirTree;
    }

    public FSDirectory() {
        // 默认刚开始就是空节点
        this.dirTree = new INode("/");
    }

    /**
     * 创建目录
     * @param path
     */
    public void mkDir(String path) {
        /**
         * path = /usr/warehouse/hive
         * 应该先判断一下 “/"根目录下有没有/usr目录的存在
         * 如果有的话需要判断 ”/usr“目录下，是否有"/usr/warehouse"目录的存在
         * 如果没有的话，需要创建一个”/warehouse“的目录，挂在"/usr"目录下
         * 接着再对"/hive"这个目录创建一个节点挂载上去
         */
        synchronized (dirTree) {
            String[] pathes = path.split("/");
            INode parent = dirTree;

            for (String splitedPath : pathes) { // ["", "usr", "warehouse", "hive"]
                if (splitedPath.trim().equals("")) {
                    continue;
                }
                INode dir = findDirectory(parent, splitedPath); // parent = "/"
                if (dir != null) {
                    parent = dir;
                    continue;
                }

                INode child = new INode(splitedPath);
                parent.addChild(child);
                parent = child;  // parent = "/usr"
            }
        }
    }

    /**
     * 创建文件
     * @param filename
     * @return
     */
    public Boolean create(String filename) {
        /**
         * /image/product/img001.jpg
         */
        synchronized (dirTree) {
            String[] splitFilename = filename.split("/");
            String realFilename = splitFilename[splitFilename.length - 1];

            INode parent = dirTree;
            for (int i = 0; i < splitFilename.length - 1; i++) {
                if (i == 0) {
                    continue;
                }
                INode dir = findDirectory(parent, splitFilename[i]);
                if (dir != null) {
                    parent = dir;
                    continue;
                }
                INode child = new INode(splitFilename[i]);
                parent.addChild(child);
                parent = child;
            }

            // 此时已经获取到了文件的上一级目录
            // 可以查找一下当前这个目录下面是否有对应的文件
            if (existFile(parent, realFilename)) {
                return false;
            }

            INode file = new INode(realFilename);
            parent.addChild(file);
            log.info(String.valueOf(dirTree));
            return true;
        }
    }

    /**
     * 目录下是否存在这个文件
     * @param dir
     * @param filename
     * @return
     */
    public Boolean existFile(INode dir, String filename) {
        if (dir.getChildren() != null && dir.getChildren().size() > 0) {
            for (INode child : dir.getChildren()) {
                if (child.getPath().equals(filename)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printDirTree(INode dirTree, String blank) {
        if (dirTree.getChildren().size() == 0) {
            return;
        }
        for (INode dir : dirTree.getChildren()) {
            System.out.println(blank + ((INode) dir).getPath());
            printDirTree((INode) dir, blank + " ");
        }
    }


    /**
     * 查找子目录
     * @param dir
     * @param path
     * @return
     */
    private INode findDirectory(INode dir, String path) {
        if (dir.getChildren().size() == 0) {
            return null;
        }
        for (INode child : dir.getChildren()) {
            if (child instanceof INode) {
                INode childDir = (INode) child;
                if ((childDir.getPath().equals(path))) {
                    return childDir;
                }
            }
        }
        return null;
    }

    public static class INode {
        private String path;
        private List<INode> children;

        public INode() {

        }

        public INode(String path) {
            this.path = path;
            this.children = new LinkedList<>();
        }

        public void addChild(INode iNode) {
            this.children.add(iNode);
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<INode> getChildren() {
            return children;
        }

        public void setChildren(List<INode> children) {
            this.children = children;
        }

        @Override
        public String toString() {
            return "INode{" +
                    "path='" + path + '\'' +
                    ", children=" + children +
                    '}';
        }
    }
}
