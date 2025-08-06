package com.easypan.TA.InputStream;

import com.easypan.TA.Config.TransferConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Component;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FtpTransferProtocol implements FileTransferProtocol {

    private FTPClient ftpClient;

    @Override
    public boolean connect(TransferConfig config) {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(config.getHost(), config.getPort());

            boolean loginSuccess = ftpClient.login(config.getUsername(), config.getPassword());
            if (loginSuccess) {
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.setControlEncoding("UTF-8");
                log.info("FTP连接成功: {}:{}", config.getHost(), config.getPort());
                return true;
            } else {
                log.error("FTP登录失败: {}:{}", config.getHost(), config.getPort());
                return false;
            }
        } catch (Exception e) {
            log.error("FTP连接异常", e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            log.error("FTP断开连接异常", e);
        }
    }

    @Override
    public List<RemoteFileInfo> listFiles(String remotePath) {
        List<RemoteFileInfo> files = new ArrayList<>();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(remotePath);
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.isFile()) {
                    RemoteFileInfo fileInfo = RemoteFileInfo.builder()
                            .fileName(ftpFile.getName())
                            .fullPath(remotePath + "/" + ftpFile.getName())
                            .size(ftpFile.getSize())
                            .lastModified(ftpFile.getTimestamp().getTimeInMillis())
                            .isDirectory(false)
                            .timestamp(FileNameParser.extractTimestamp(ftpFile.getName()))
                            .build();
                    files.add(fileInfo);
                }
            }
        } catch (Exception e) {
            log.error("FTP列出文件异常", e);
        }
        return files;
    }

    @Override
    public boolean downloadFile(String remoteFilePath, String localFilePath) {
        try {
            FileOutputStream fos = new FileOutputStream(localFilePath);
            boolean success = ftpClient.retrieveFile(remoteFilePath, fos);
            fos.close();
            if (success) {
                log.info("FTP下载文件成功: {} -> {}", remoteFilePath, localFilePath);
            }
            return success;
        } catch (Exception e) {
            log.error("FTP下载文件异常", e);
            return false;
        }
    }

    @Override
    public InputStream getFileStream(String remoteFilePath) {
        try {
            return ftpClient.retrieveFileStream(remoteFilePath);
        } catch (Exception e) {
            log.error("FTP获取文件流异常", e);
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return ftpClient != null && ftpClient.isConnected();
    }

    @Override
    public TransferProtocolType getProtocolType() {
        return TransferProtocolType.FTP;
    }
}
