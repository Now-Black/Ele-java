package com.easypan.entity.query;

import java.util.Date;

/**
 * 客户每日收益查询条件
 */
public class ClientDailyIncomeQuery extends BaseParam {
    
    private String clientNo;
    private String prdCode;
    private Date regDate;
    private Date regDateStart;
    private Date regDateEnd;

    public String getClientNo() {
        return clientNo;
    }

    public void setClientNo(String clientNo) {
        this.clientNo = clientNo;
    }

    public String getPrdCode() {
        return prdCode;
    }

    public void setPrdCode(String prdCode) {
        this.prdCode = prdCode;
    }

    public Date getRegDate() {
        return regDate;
    }

    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }

    public Date getRegDateStart() {
        return regDateStart;
    }

    public void setRegDateStart(Date regDateStart) {
        this.regDateStart = regDateStart;
    }

    public Date getRegDateEnd() {
        return regDateEnd;
    }

    public void setRegDateEnd(Date regDateEnd) {
        this.regDateEnd = regDateEnd;
    }
}