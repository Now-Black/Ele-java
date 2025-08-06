package com.easypan.TA.Config;

import com.easypan.TA.InputStream.FileTransferProtocol;
import com.easypan.TA.InputStream.FtpTransferProtocol;
import com.easypan.TA.InputStream.SftpTransferProtocol;
import com.easypan.TA.InputStream.TransferProtocolType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileTransferProtocolFactory {

    @Autowired
    private FtpTransferProtocol ftpProtocol;

    @Autowired
    private SftpTransferProtocol sftpProtocol;

    public FileTransferProtocol createProtocol(TransferProtocolType protocolType) {
        switch (protocolType) {
            case FTP:
                return ftpProtocol;
            case SFTP_PASSWORD:
            case SFTP_PRIVATE_KEY:
                return sftpProtocol;
            default:
                throw new IllegalArgumentException("不支持的传输协议: " + protocolType);
        }
    }
}

