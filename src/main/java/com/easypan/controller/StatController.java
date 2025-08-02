package com.easypan.controller;

import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.StatService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/stat")
public class StatController extends ABaseController {
    @Resource
    private StatService statService;

    // 获取热点内容列表
    @RequestMapping(value = "/hotList", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseVO getHotList(@RequestParam(defaultValue = "7") Integer days) {
        return getSuccessResponseVO(statService.getHotList(days));
    }

    // 获取置顶内容列表
    @RequestMapping(value = "/topList", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseVO getTopList() {
        return getSuccessResponseVO(statService.getTopList());
    }

    // 设置/取消置顶内容
    @PostMapping("/setTop")
    public ResponseVO setTop(@RequestParam String fileId, @RequestParam Integer weight) {
        statService.setTop(fileId, weight);
        return getSuccessResponseVO(null);
    }
} 