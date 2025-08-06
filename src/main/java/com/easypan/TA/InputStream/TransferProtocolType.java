package com.easypan.TA.InputStream;

public enum TransferProtocolType {
    FTP("ftp"),
    SFTP_PASSWORD("sftp_password"),
    SFTP_PRIVATE_KEY("sftp_private_key");

    private final String code;

    TransferProtocolType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
