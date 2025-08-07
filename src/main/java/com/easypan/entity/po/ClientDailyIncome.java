package com.easypan.entity.po;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户每日收益表
 */
public class ClientDailyIncome implements Serializable {
    
    private String clientNo;
    private String assetAcc;
    private String taClient;
    private String prdCode;
    private String realPrdCode;
    private Date regDate;
    private BigDecimal allotAmt;
    private BigDecimal redeemAmt;
    private BigDecimal divIncome;
    private BigDecimal redeemIncome;
    private BigDecimal forceAddAmt;
    private Date createTime;
    private Date updateTime;

    public String getClientNo() {
        return clientNo;
    }

    public void setClientNo(String clientNo) {
        this.clientNo = clientNo;
    }

    public String getAssetAcc() {
        return assetAcc;
    }

    public void setAssetAcc(String assetAcc) {
        this.assetAcc = assetAcc;
    }

    public String getTaClient() {
        return taClient;
    }

    public void setTaClient(String taClient) {
        this.taClient = taClient;
    }

    public String getPrdCode() {
        return prdCode;
    }

    public void setPrdCode(String prdCode) {
        this.prdCode = prdCode;
    }

    public String getRealPrdCode() {
        return realPrdCode;
    }

    public void setRealPrdCode(String realPrdCode) {
        this.realPrdCode = realPrdCode;
    }

    public Date getRegDate() {
        return regDate;
    }

    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }

    public BigDecimal getAllotAmt() {
        return allotAmt;
    }

    public void setAllotAmt(BigDecimal allotAmt) {
        this.allotAmt = allotAmt;
    }

    public BigDecimal getRedeemAmt() {
        return redeemAmt;
    }

    public void setRedeemAmt(BigDecimal redeemAmt) {
        this.redeemAmt = redeemAmt;
    }

    public BigDecimal getDivIncome() {
        return divIncome;
    }

    public void setDivIncome(BigDecimal divIncome) {
        this.divIncome = divIncome;
    }

    public BigDecimal getRedeemIncome() {
        return redeemIncome;
    }

    public void setRedeemIncome(BigDecimal redeemIncome) {
        this.redeemIncome = redeemIncome;
    }

    public BigDecimal getForceAddAmt() {
        return forceAddAmt;
    }

    public void setForceAddAmt(BigDecimal forceAddAmt) {
        this.forceAddAmt = forceAddAmt;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}