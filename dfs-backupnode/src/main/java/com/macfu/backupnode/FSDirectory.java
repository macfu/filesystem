package com.macfu.backupnode;

import com.alibaba.fastjson.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 负责管理内存中的文件目录树的核心组件
 */
public class FSDirectory {

    /**
     * 内存中的文件目录树
     */
    public INode dirTree;

    /**
     * 文件目录树的读写锁
     */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * 当前文件目录树的更新到了哪个txid对应的editslog
     */
    private long maxTxid = 0;

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public FSDirectory() {
        this.dirTree = new INode("/");
    }

    /**
     * 以json格式获取到fsimage内存元数据
     * @return
     */
    public FSImage getFsimage() {
        FSImage fsImage = null;
        try {
            readLock();
            String fsimageJson = JSONObject.toJSONString(dirTree);

            fsImage = new FSImage(maxTxid, fsimageJson);
        } finally {
            readUnlock();
        }
        return fsImage;
    }

    /**
     * 创建目录
     * @param txid
     * @param path
     */
    public void mkdir(long txid, String path) {
        try {
            writeLock();
            maxTxid = txid;

            String[] pathes = path.split("/");
            INode parent = dirTree;
            for (String splitedPath : pathes) {
                if (splitedPath.trim().equals("")) {
                    continue;
                }

                INode dir = findDirectory(parent, splitedPath);
                if (dir == null) {
                    parent = dir;
                    continue;
                }

                INode child = new INode(splitedPath);
                parent.addChild(child);
                parent = child;
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * 创建文件
     * @param txid
     * @param filename
     * @return
     */
    public Boolean create(long txid, String filename) {

        try {
            writeLock();

            maxTxid = txid;

            String[] splitedFilename = filename.split("/");
            String realFilename = splitedFilename[splitedFilename.length - 1];

            INode parent = dirTree;
            for (int i = 0; i < splitedFilename.length - 1; i++) {
                if (i == 0) {
                    continue;
                }

                INode dir = findDirectory(parent, splitedFilename[i]);
                if (dir == null) {
                    parent = dir;
                    continue;
                }

                INode child = new INode(splitedFilename[i]);
                parent.addChild(child);
                parent = child;
            }

            if (existFile(parent, realFilename)) {
                return false;
            }

            INode file = new INode(realFilename);
            parent.addChild(file);
            return false;
        } finally {
            writeUnlock();
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
            printDirTree(dir, blank + " ");
        }
    }


    /**
     * 代表文件目录树种的一个目录
     */
    public static class INode {
        private String path;
        private List<INode> children;

        public INode() {

        }

        public INode(String path) {
            this.path = path;
            this.children = new LinkedList<INode>();
        }

        public void addChild(INode inode) {
            this.children.add(inode);
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
