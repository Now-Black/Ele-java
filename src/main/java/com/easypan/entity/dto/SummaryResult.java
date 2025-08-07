package com.easypan.entity.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 汇总结果
 */
public class SummaryResult {
    private Date calculateDate;
    private long totalTime;
    private int totalTables = 16;
    private int successCount = 0;
    private int failureCount = 0;
    private long totalAffectedRows = 0;
    
    private Map<Integer, TableSummaryResult> tableResults = new ConcurrentHashMap<>();
    private List<String> failures = new ArrayList<>();

    public SummaryResult(Date calculateDate) {
        this.calculateDate = calculateDate;
    }

    public void addTableResult(int tableIndex, TableSummaryResult result) {
        tableResults.put(tableIndex, result);
        
        if (result.isSuccess()) {
            successCount++;
            totalAffectedRows += result.getAffectedRows();
        } else {
            failureCount++;
        }
    }

    public void addFailure(int tableIndex, String errorMessage) {
        String failureInfo = String.format("分表%d: %s", tableIndex, errorMessage);
        failures.add(failureInfo);
        failureCount++;
    }

    public boolean isAllSuccess() {
        return failureCount == 0;
    }

    public double getSuccessRate() {
        return totalTables == 0 ? 0.0 : (double) successCount / totalTables;
    }

    // Getters and Setters
    public Date getCalculateDate() {
        return calculateDate;
    }

    public void setCalculateDate(Date calculateDate) {
        this.calculateDate = calculateDate;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public int getTotalTables() {
        return totalTables;
    }

    public void setTotalTables(int totalTables) {
        this.totalTables = totalTables;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public long getTotalAffectedRows() {
        return totalAffectedRows;
    }

    public void setTotalAffectedRows(long totalAffectedRows) {
        this.totalAffectedRows = totalAffectedRows;
    }

    public Map<Integer, TableSummaryResult> getTableResults() {
        return tableResults;
    }

    public void setTableResults(Map<Integer, TableSummaryResult> tableResults) {
        this.tableResults = tableResults;
    }

    public List<String> getFailures() {
        return failures;
    }

    public void setFailures(List<String> failures) {
        this.failures = failures;
    }
}