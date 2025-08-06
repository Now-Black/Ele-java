package com.easypan.TA.InputStream;

import com.easypan.TA.Config.TransferConfig;
import java.io.InputStream;
import java.util.List;

public interface FileTransferProtocol {

    /**
     * 连接到远程服务器
     */
    boolean connect(TransferConfig config);

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 列出远程目录下的文件
     */
    List<RemoteFileInfo> listFiles(String remotePath);

    /**
     * 下载文件到本地
     */
    boolean downloadFile(String remoteFilePath, String localFilePath);

    /**
     * 获取文件输入流
     */
    InputStream getFileStream(String remoteFilePath);

    /**
     * 检查连接状态
     */
    boolean isConnected();

    /**
     * 获取协议类型
     */
    TransferProtocolType getProtocolType();
}




