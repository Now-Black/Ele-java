package com.easypan.TA.Config;

import com.easypan.TA.InputStream.TransferProtocolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferConfig {
    private String configId;
    private String host;
    private int port;
    private String username;
    private String password;
    private String remotePath;
    private String localPath;
    private TransferProtocolType protocolType;

    // SFTP私钥认证专用
    private String privateKeyPath;
    private String passphrase;

    // 其他配置
    private int timeout = 30000;
    private String encoding = "UTF-8";
    private boolean enabled = true;

    // 文件过滤规则
    private List<String> filePatterns;
    private long createTime;
    private long updateTime;
}
