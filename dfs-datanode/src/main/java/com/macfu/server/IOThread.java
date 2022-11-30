package com.macfu.server;

import com.macfu.server.util.DataNodeConfig;
import com.macfu.server.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 负责执行磁盘IO的线程
 */
@Slf4j
public class IOThread extends Thread {

    public static final Integer REQUEST_SEND_FILE = 1;
    public static final Integer REQUEST_READ_FILE = 2;

    private NetWorkRequestQueue requestQueue = NetWorkRequestQueue.get();
    private NameNodeRpcClient namenode;

    public IOThread(NameNodeRpcClient namenode) {
        this.namenode = namenode;
    }

    @Override
    public void run() {
        while (true) {
            try {
                NetWorkRequest request = requestQueue.poll();
                if (requestQueue == null) {
                    Thread.sleep(100);
                    continue;
                }

                Integer requestType = request.getRequestType();
                if (REQUEST_SEND_FILE.equals(requestType)) {
                    writeFileToLocalDisk(request);
                } else if (REQUEST_READ_FILE.equals(requestType)) {
                    readFileFromLocalDisk(request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void readFileFromLocalDisk(NetWorkRequest request) throws Exception {
        FileInputStream inputStream = null;
        FileChannel localFileChannel = null;
        try {
            File file = new File(request.getAbsoluteFilename());
            Long fileLength = file.length();

            String absoluteFilename = FileUtils.getAbsoluteFilename(request.getRelativeFilename());
            log.info("准备读取的文件路径：" + absoluteFilename);

            inputStream = new FileInputStream(absoluteFilename);
            localFileChannel = inputStream.getChannel();

            // 循环不断的从channel里读取数据
            ByteBuffer buffer = ByteBuffer.allocate(8 + Integer.valueOf(String.valueOf(fileLength)));
            buffer.putLong(fileLength);
            int hasReadImageLength = localFileChannel.read(buffer);
            log.info("从本次磁盘文件文件中读取了" + hasReadImageLength + "bytes的数据");

            buffer.rewind();

            // 封装响应
            NetWorkResponse response = new NetWorkResponse();
            response.setClient(request.getClient());
            response.setBuffer(buffer);

            NetWorkResponseQueue responseQueue = NetWorkResponseQueue.get();
            responseQueue.offer(request.getProcessorId(), response);
        } finally {
            if (localFileChannel != null) {
                localFileChannel.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void writeFileToLocalDisk(NetWorkRequest request) throws Exception {
        FileOutputStream outputStream = null;
        FileChannel localFileChannel = null;
        try {
            log.info("准备写文件：" + DataNodeConfig.DATA_DIR);

            String absoluteFilename = FileUtils.getAbsoluteFilename(request.getRelativeFilename());
            log.info("准备写入文件的路径：" + absoluteFilename);

            outputStream = new FileOutputStream(absoluteFilename);
            localFileChannel = outputStream.getChannel();
            localFileChannel.position(localFileChannel.size());

            int written = localFileChannel.write(request.getFile());
            log.info("本次文件上传完毕，将" + written + "bytes的数据写入本次磁盘文件");

            namenode.informReplicaReceived(request.getRelativeFilename() + "_" + request.getFileLength());
            log.info("增量上报收到的文件副本给namenode节点......");

            NetWorkResponse response = new NetWorkResponse();
            response.setClient(request.getClient());
            response.setBuffer(ByteBuffer.wrap("SUCCESS".getBytes()));

            NetWorkResponseQueue responseQueue = NetWorkResponseQueue.get();
            responseQueue.offer(request.getProcessorId(), response);
        } finally {
            localFileChannel.close();
            outputStream.close();
        }
    }
}
