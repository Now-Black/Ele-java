package com.easypan.TA.InputStream;


import com.easypan.TA.Config.TransferConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Component
@Slf4j
public class SftpTransferProtocol implements FileTransferProtocol {

    private JSch jsch;
    private Session session;
    private ChannelSftp sftpChannel;

    @Override
    public boolean connect(TransferConfig config) {
        try {
            jsch = new JSch();

            // 如果是私钥认证
            if (config.getProtocolType() == TransferProtocolType.SFTP_PRIVATE_KEY) {
                if (StringUtils.isNotBlank(config.getPrivateKeyPath())) {
                    if (StringUtils.isNotBlank(config.getPassphrase())) {
                        jsch.addIdentity(config.getPrivateKeyPath(), config.getPassphrase());
                    } else {
                        jsch.addIdentity(config.getPrivateKeyPath());
                    }
                }
            }

            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());

            // 如果是密码认证
            if (config.getProtocolType() == TransferProtocolType.SFTP_PASSWORD) {
                session.setPassword(config.getPassword());
            }

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000); // 30秒超时

            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;

            log.info("SFTP连接成功: {}:{}", config.getHost(), config.getPort());
            return true;

        } catch (Exception e) {
            log.error("SFTP连接异常", e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } catch (Exception e) {
            log.error("SFTP断开连接异常", e);
        }
    }

    @Override
    public List<RemoteFileInfo> listFiles(String remotePath) {
        List<RemoteFileInfo> files = new ArrayList<>();
        try {
            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(remotePath);
            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getAttrs().isDir() && !entry.getFilename().startsWith(".")) {
                    RemoteFileInfo fileInfo = RemoteFileInfo.builder()
                            .fileName(entry.getFilename())
                            .fullPath(remotePath + "/" + entry.getFilename())
                            .size(entry.getAttrs().getSize())
                            .lastModified(entry.getAttrs().getMTime() * 1000L)
                            .isDirectory(false)
                            .timestamp(FileNameParser.extractTimestamp(entry.getFilename()))
                            .build();
                    files.add(fileInfo);
                }
            }
        } catch (Exception e) {
            log.error("SFTP列出文件异常", e);
        }
        return files;
    }

    @Override
    public boolean downloadFile(String remoteFilePath, String localFilePath) {
        try {
            sftpChannel.get(remoteFilePath, localFilePath);
            log.info("SFTP下载文件成功: {} -> {}", remoteFilePath, localFilePath);
            return true;
        } catch (Exception e) {
            log.error("SFTP下载文件异常", e);
            return false;
        }
    }

    @Override
    public InputStream getFileStream(String remoteFilePath) {
        try {
            return sftpChannel.get(remoteFilePath);
        } catch (Exception e) {
            log.error("SFTP获取文件流异常", e);
            return null;
        }
    }

    @Override
    public boolean isConnected() {
        return sftpChannel != null && sftpChannel.isConnected() &&
                session != null && session.isConnected();
    }

    @Override
    public TransferProtocolType getProtocolType() {
        return TransferProtocolType.SFTP_PASSWORD; // 在工厂中设置具体类型
    }
}

