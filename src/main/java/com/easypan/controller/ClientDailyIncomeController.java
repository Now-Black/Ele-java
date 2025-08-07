package com.easypan.controller;

import com.easypan.entity.po.ClientDailyIncome;
import com.easypan.entity.query.ClientDailyIncomeQuery;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.ClientDailyIncomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 客户每日收益Controller
 */
@RestController("clientDailyIncomeController")
@RequestMapping("/clientDailyIncome")
public class ClientDailyIncomeController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ClientDailyIncomeController.class);

    @Resource
    private ClientDailyIncomeService clientDailyIncomeService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadDataList")
    public ResponseVO loadDataList(ClientDailyIncomeQuery query) {
        return getSuccessResponseVO(clientDailyIncomeService.findListByPage(query));
    }

    /**
     * 新增客户收益记录
     */
    @RequestMapping("/add")
    public ResponseVO add(@RequestBody ClientDailyIncome clientDailyIncome) {
        clientDailyIncomeService.add(clientDailyIncome);
        return getSuccessResponseVO(null);
    }

    /**
     * 执行客户每日收益计算
     */
    @PostMapping("/calculate")
    public ResponseVO calculateDailyIncome(@RequestParam String calculateDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(calculateDate);
            
            logger.info("开始执行客户每日收益计算，计算日期：{}", calculateDate);
            clientDailyIncomeService.calculateClientDailyIncome(date);
            
            return getSuccessResponseVO("客户每日收益计算完成");
            
        } catch (ParseException e) {
            logger.error("日期格式错误：{}", calculateDate, e);
            return getBusinessErrorResponseVO("日期格式错误，请使用yyyy-MM-dd格式");
        } catch (Exception e) {
            logger.error("客户每日收益计算异常", e);
            return getBusinessErrorResponseVO("收益计算失败：" + e.getMessage());
        }
    }

    /**
     * 按客户编号查询收益明细
     */
    @GetMapping("/getByClientNo")
    public ResponseVO getByClientNo(@RequestParam String clientNo,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate) {
        try {
            ClientDailyIncomeQuery query = new ClientDailyIncomeQuery();
            query.setClientNo(clientNo);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (startDate != null && !startDate.isEmpty()) {
                query.setRegDateStart(sdf.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                query.setRegDateEnd(sdf.parse(endDate));
            }
            
            PaginationResultVO<ClientDailyIncome> result = clientDailyIncomeService.findListByPage(query);
            return getSuccessResponseVO(result);
            
        } catch (ParseException e) {
            logger.error("日期格式错误", e);
            return getBusinessErrorResponseVO("日期格式错误，请使用yyyy-MM-dd格式");
        } catch (Exception e) {
            logger.error("查询客户收益明细异常", e);
            return getBusinessErrorResponseVO("查询失败：" + e.getMessage());
        }
    }

    /**
     * 按产品代码查询收益汇总
     */
    @GetMapping("/getByPrdCode")
    public ResponseVO getByPrdCode(@RequestParam String prdCode,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        try {
            ClientDailyIncomeQuery query = new ClientDailyIncomeQuery();
            query.setPrdCode(prdCode);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (startDate != null && !startDate.isEmpty()) {
                query.setRegDateStart(sdf.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                query.setRegDateEnd(sdf.parse(endDate));
            }
            
            PaginationResultVO<ClientDailyIncome> result = clientDailyIncomeService.findListByPage(query);
            return getSuccessResponseVO(result);
            
        } catch (ParseException e) {
            logger.error("日期格式错误", e);
            return getBusinessErrorResponseVO("日期格式错误，请使用yyyy-MM-dd格式");
        } catch (Exception e) {
            logger.error("查询产品收益汇总异常", e);
            return getBusinessErrorResponseVO("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取收益计算状态（可用于监控任务进度）
     */
    @GetMapping("/getCalculateStatus")
    public ResponseVO getCalculateStatus() {
        // 这里可以实现计算状态查询逻辑
        // 比如查询Redis中的任务状态
        return getSuccessResponseVO("计算状态查询功能待实现");
    }
}