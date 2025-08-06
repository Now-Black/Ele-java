package com.easypan.TA.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResult {
    private boolean success;
    private String message;
    private String configId;
    private String fileName;
    private String localPath;
    private long fileSize;
    private long transferTime;
    private Exception exception;

    public static TransferResult success(String configId, String fileName, String localPath, long fileSize, long transferTime) {
        return TransferResult.builder()
                .success(true)
                .configId(configId)
                .fileName(fileName)
                .localPath(localPath)
                .fileSize(fileSize)
                .transferTime(transferTime)
                .message("传输成功")
                .build();
    }

    public static TransferResult failure(String configId, String fileName, String message, Exception exception) {
        return TransferResult.builder()
                .success(false)
                .configId(configId)
                .fileName(fileName)
                .message(message)
                .exception(exception)
                .build();
    }
}
