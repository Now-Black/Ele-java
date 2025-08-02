package com.easypan.service.impl;

import com.easypan.component.RedisComponent;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.service.StatService;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.UserInfo;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatServiceImpl implements StatService {
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private FileInfoMapper<FileInfo, Object> fileInfoMapper;
    @Autowired
    private UserInfoMapper<UserInfo, Object> userInfoMapper;

    private static final String HOT_HASH_KEY = "stat:hot:file";
    private static final String TOP_ZSET_KEY = "stat:top:file";

    @Override
    public List<Object> getHotList(Integer days) {
        // 假设Redis Hash结构: key=HOT_HASH_KEY, field=fileId, value=访问量
        Map<String, String> hotMap = redisComponent.getHashAll(HOT_HASH_KEY);
        // 取访问量前10的文件ID
        List<Map.Entry<String, String>> sorted = hotMap.entrySet().stream()
                .sorted((a, b) -> Integer.parseInt(b.getValue()) - Integer.parseInt(a.getValue()))
                .limit(10)
                .collect(Collectors.toList());
        // 这里只返回fileId和count，实际可扩展为FileInfoVO等
        List<Object> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : sorted) {
            String fileId = entry.getKey();
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, null);
            String fileName = fileInfo != null ? fileInfo.getFileName() : "未知文件";
            String userId = fileInfo != null ? fileInfo.getUserId() : null;
            String userName = null;
            if(userId != null) {
                UserInfo user = userInfoMapper.selectByUserId(userId);
                userName = user != null ? user.getNickName() : null;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("fileId", fileId);
            map.put("fileName", fileName);
            map.put("count", entry.getValue());
            map.put("userId", userId);
            map.put("userName", userName);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Object> getTopList() {
        // 假设Redis ZSet结构: key=TOP_ZSET_KEY, value=fileId, score=权重
        Set<String> topSet = redisComponent.getZSetRevRange(TOP_ZSET_KEY, 0, 9);
        List<Object> result = new ArrayList<>();
        for (String fileId : topSet) {
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, null);
            String fileName = fileInfo != null ? fileInfo.getFileName() : "未知文件";
            String userId = fileInfo != null ? fileInfo.getUserId() : null;
            String userName = null;
            if(userId != null) {
                UserInfo user = userInfoMapper.selectByUserId(userId);
                userName = user != null ? user.getNickName() : null;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("fileId", fileId);
            map.put("fileName", fileName);
            map.put("weight", redisComponent.getZSetScore(TOP_ZSET_KEY, fileId));
            map.put("userId", userId);
            map.put("userName", userName);
            result.add(map);
        }
        return result;
    }

    @Override
    public void setTop(String fileId, Integer weight) {
        if (weight == null || weight <= 0) {
            redisComponent.removeZSet(TOP_ZSET_KEY, fileId);
        } else {
            redisComponent.addZSet(TOP_ZSET_KEY, fileId, weight);
        }
    }
} 