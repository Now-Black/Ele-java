package com.easypan.TA.InputStream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteFileInfo {
    private String fileName;
    private String fullPath;
    private long size;
    private long lastModified;
    private boolean isDirectory;
    private String timestamp; // 从文件名解析的时间戳
}
