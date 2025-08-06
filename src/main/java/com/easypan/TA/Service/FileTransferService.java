package com.easypan.TA.Service;

import com.easypan.TA.Config.FileTransferProtocolFactory;
import com.easypan.TA.Config.TransferConfig;
import com.easypan.TA.Config.TransferConfigService;
import com.easypan.TA.Entity.ScanResult;
import com.easypan.TA.Entity.TransferResult;
import com.easypan.TA.InputStream.FileNameParser;
import com.easypan.TA.InputStream.FileTransferProtocol;
import com.easypan.TA.InputStream.RemoteFileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileTransferService {

    @Autowired
    private TransferConfigService configService;

    @Autowired
    private FileTransferProtocolFactory protocolFactory;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LAST_SYNC_KEY_PREFIX = "last_sync_time:";
    private static final String PROCESSED_FILES_KEY_PREFIX = "processed_files:";

    /**
     * 手动触发文件传输
     */
    public List<TransferResult> manualTransfer(String configId) {
        TransferConfig config = configService.getConfig(configId);
        if (config == null) {
            return Arrays.asList(TransferResult.failure(configId, null, "配置不存在", null));
        }

        ScanResult scanResult = scanAndDownloadFiles(config);
        return scanResult.getTransferResults();
    }

    /**
     * 扫描并下载文件
     */
    public ScanResult scanAndDownloadFiles(TransferConfig config) {
        long startTime = System.currentTimeMillis();

        FileTransferProtocol protocol = protocolFactory.createProtocol(config.getProtocolType());
        List<TransferResult> transferResults = new ArrayList<>();

        try {
            // 连接到远程服务器
            if (!protocol.connect(config)) {
                return ScanResult.builder()
                        .configId(config.getConfigId())
                        .success(false)
                        .message("连接远程服务器失败")
                        .scanTime(System.currentTimeMillis() - startTime)
                        .transferResults(Collections.emptyList())
                        .build();
            }

            // 扫描远程文件
            List<RemoteFileInfo> remoteFiles = protocol.listFiles(config.getRemotePath());

            // 过滤有效文件
            List<RemoteFileInfo> validFiles = remoteFiles.stream()
                    .filter(this::isValidFile)
                    .collect(Collectors.toList());

            // 获取需要下载的新文件
            List<RemoteFileInfo> newFiles = getNewFiles(config.getConfigId(), validFiles);

            // 下载文件
            for (RemoteFileInfo fileInfo : newFiles) {
                TransferResult result = downloadSingleFile(protocol, config, fileInfo);
                transferResults.add(result);

                // 记录已处理文件
                if (result.isSuccess()) {
                    markFileAsProcessed(config.getConfigId(), fileInfo.getFileName());
                }
            }

            // 更新最后同步时间
            updateLastSyncTime(config.getConfigId());

            return ScanResult.builder()
                    .configId(config.getConfigId())
                    .totalFiles(validFiles.size())
                    .newFiles(newFiles.size())
                    .newFileList(newFiles)
                    .transferResults(transferResults)
                    .scanTime(System.currentTimeMillis() - startTime)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("扫描下载文件异常", e);
            return ScanResult.builder()
                    .configId(config.getConfigId())
                    .success(false)
                    .message("扫描下载异常: " + e.getMessage())
                    .scanTime(System.currentTimeMillis() - startTime)
                    .transferResults(transferResults)
                    .build();
        } finally {
            protocol.disconnect();
        }
    }

    /**
     * 下载单个文件
     */
    private TransferResult downloadSingleFile(FileTransferProtocol protocol, TransferConfig config, RemoteFileInfo fileInfo) {
        long startTime = System.currentTimeMillis();

        try {
            // 确保本地目录存在
            File localDir = new File(config.getLocalPath());
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            String localFilePath = config.getLocalPath() + File.separator + fileInfo.getFileName();

            // 下载文件
            boolean success = protocol.downloadFile(fileInfo.getFullPath(), localFilePath);

            if (success) {
                long transferTime = System.currentTimeMillis() - startTime;
                log.info("文件下载成功: {} -> {}, 耗时: {}ms", fileInfo.getFullPath(), localFilePath, transferTime);

                return TransferResult.success(
                        config.getConfigId(),
                        fileInfo.getFileName(),
                        localFilePath,
                        fileInfo.getSize(),
                        transferTime
                );
            } else {
                return TransferResult.failure(
                        config.getConfigId(),
                        fileInfo.getFileName(),
                        "下载失败",
                        null
                );
            }

        } catch (Exception e) {
            log.error("下载文件异常: {}", fileInfo.getFileName(), e);
            return TransferResult.failure(
                    config.getConfigId(),
                    fileInfo.getFileName(),
                    "下载异常: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 检查是否为有效文件
     */
    private boolean isValidFile(RemoteFileInfo fileInfo) {
        if (fileInfo.isDirectory()) {
            return false;
        }

        FileNameParser.ParsedFileName parsed = FileNameParser.parseFileName(fileInfo.getFileName());
        return parsed.isValid();
    }

    /**
     * 获取需要下载的新文件
     */
    private List<RemoteFileInfo> getNewFiles(String configId, List<RemoteFileInfo> remoteFiles) {
        Set<String> processedFiles = getProcessedFiles(configId);

        // 按文件类型分组
        Map<FileNameParser.FileType, List<RemoteFileInfo>> filesByType = remoteFiles.stream()
                .collect(Collectors.groupingBy(file -> {
                    FileNameParser.ParsedFileName parsed = FileNameParser.parseFileName(file.getFileName());
                    return parsed.getFileType();
                }));

        List<RemoteFileInfo> newFiles = new ArrayList<>();

        // 每种类型只取最新的文件
        for (Map.Entry<FileNameParser.FileType, List<RemoteFileInfo>> entry : filesByType.entrySet()) {
            List<RemoteFileInfo> filesOfType = entry.getValue();

            // 按时间戳排序，取最新的
            RemoteFileInfo latestFile = filesOfType.stream()
                    .max(Comparator.comparing(file -> {
                        FileNameParser.ParsedFileName parsed = FileNameParser.parseFileName(file.getFileName());
                        return parsed.getParseDate();
                    }))
                    .orElse(null);

            if (latestFile != null && !processedFiles.contains(latestFile.getFileName())) {
                newFiles.add(latestFile);
            }
        }

        return newFiles;
    }

    /**
     * 获取已处理的文件列表
     */
    private Set<String> getProcessedFiles(String configId) {
        String key = PROCESSED_FILES_KEY_PREFIX + configId;
        Set<Object> processedFiles = redisTemplate.opsForSet().members(key);

        if (processedFiles == null) {
            return new HashSet<>();
        }

        return processedFiles.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 标记文件为已处理
     */
    private void markFileAsProcessed(String configId, String fileName) {
        String key = PROCESSED_FILES_KEY_PREFIX + configId;
        redisTemplate.opsForSet().add(key, fileName);
        // 设置30天过期
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    /**
     * 更新最后同步时间
     */
    private void updateLastSyncTime(String configId) {
        String key = LAST_SYNC_KEY_PREFIX + configId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis());
    }

    /**
     * 获取最后同步时间
     */
    public Long getLastSyncTime(String configId) {
        String key = LAST_SYNC_KEY_PREFIX + configId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.valueOf(value.toString()) : null;
    }
}

