package com.easypan.entity.dto;

/**
 * 单表汇总结果
 */
public class TableSummaryResult {
    private int tableIndex;
    private String tableName;
    private int affectedRows;
    private long processTime;
    private boolean success;
    private String errorMessage;

    public static TableSummaryResult success(int tableIndex, String tableName, int affectedRows, long processTime) {
        TableSummaryResult result = new TableSummaryResult();
        result.tableIndex = tableIndex;
        result.tableName = tableName;
        result.affectedRows = affectedRows;
        result.processTime = processTime;
        result.success = true;
        return result;
    }

    public static TableSummaryResult failure(int tableIndex, String tableName, String errorMessage, long processTime) {
        TableSummaryResult result = new TableSummaryResult();
        result.tableIndex = tableIndex;
        result.tableName = tableName;
        result.affectedRows = 0;
        result.processTime = processTime;
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    // Getters and Setters
    public int getTableIndex() {
        return tableIndex;
    }

    public void setTableIndex(int tableIndex) {
        this.tableIndex = tableIndex;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}