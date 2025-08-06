package com.easypan.TA.Config;

import com.easypan.TA.InputStream.FileTransferProtocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TransferConfigService {

    private static final String CONFIG_KEY_PREFIX = "file_transfer_config:";
    private static final String CONFIG_LIST_KEY = "file_transfer_configs";
    @Resource
    private FileTransferProtocolFactory fileTransferProtocolFactory;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 保存传输配置
     */
    public void saveConfig(TransferConfig config) {
        try {
            config.setUpdateTime(System.currentTimeMillis());
            if (config.getCreateTime() == 0) {
                config.setCreateTime(System.currentTimeMillis());
            }

            String configKey = CONFIG_KEY_PREFIX + config.getConfigId();
            String configJson = objectMapper.writeValueAsString(config);

            redisTemplate.opsForValue().set(configKey, configJson);
            redisTemplate.opsForSet().add(CONFIG_LIST_KEY, config.getConfigId());

            log.info("保存传输配置成功: {}", config.getConfigId());
        } catch (Exception e) {
            log.error("保存传输配置异常", e);
            throw new RuntimeException("保存传输配置失败", e);
        }
    }

    /**
     * 获取传输配置
     */
    public TransferConfig getConfig(String configId) {
        try {
            String configKey = CONFIG_KEY_PREFIX + configId;
            String configJson = (String) redisTemplate.opsForValue().get(configKey);

            if (StringUtils.isBlank(configJson)) {
                return null;
            }

            return objectMapper.readValue(configJson, TransferConfig.class);
        } catch (Exception e) {
            log.error("获取传输配置异常", e);
            return null;
        }
    }

    /**
     * 获取所有启用的配置
     */
    public List<TransferConfig> getAllEnabledConfigs() {
        try {
            Set<Object> configIds = redisTemplate.opsForSet().members(CONFIG_LIST_KEY);
            List<TransferConfig> configs = new ArrayList<>();

            if (configIds != null) {
                for (Object configId : configIds) {
                    TransferConfig config = getConfig(String.valueOf(configId));
                    if (config != null && config.isEnabled()) {
                        configs.add(config);
                    }
                }
            }

            return configs;
        } catch (Exception e) {
            log.error("获取所有传输配置异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String configId) {
        try {
            String configKey = CONFIG_KEY_PREFIX + configId;
            redisTemplate.delete(configKey);
            redisTemplate.opsForSet().remove(CONFIG_LIST_KEY, configId);

            log.info("删除传输配置成功: {}", configId);
        } catch (Exception e) {
            log.error("删除传输配置异常", e);
        }
    }

    /**
     * 更新配置状态
     */
    public void updateConfigStatus(String configId, boolean enabled) {
        TransferConfig config = getConfig(configId);
        if (config != null) {
            config.setEnabled(enabled);
            saveConfig(config);
        }
    }

    /**
     * 测试连接
     */
    public boolean testConnection(TransferConfig config) {
        FileTransferProtocol protocol = fileTransferProtocolFactory.createProtocol(config.getProtocolType());
        try {
            return protocol.connect(config);
        } finally {
            protocol.disconnect();
        }
    }
}

