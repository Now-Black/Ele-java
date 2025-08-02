package com.easypan.service;

import java.util.List;

public interface StatService {
    List<Object> getHotList(Integer days);
    List<Object> getTopList();
    void setTop(String fileId, Integer weight);
} 