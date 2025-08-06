package com.easypan.TA.Entity;

import com.easypan.TA.InputStream.RemoteFileInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResult {
    private String configId;
    private int totalFiles;
    private int newFiles;
    private List<RemoteFileInfo> newFileList;
    private List<TransferResult> transferResults;
    private long scanTime;
    private boolean success;
    private String message;
}
