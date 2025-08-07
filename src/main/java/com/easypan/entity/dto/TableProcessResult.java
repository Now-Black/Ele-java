package com.easypan.entity.dto;

/**
 * 分表处理结果
 */
public class TableProcessResult {
    private int tableIndex;
    private boolean success;
    private Exception error;
    private long processTime;

    private TableProcessResult(int tableIndex, boolean success, Exception error, long processTime) {
        this.tableIndex = tableIndex;
        this.success = success;
        this.error = error;
        this.processTime = processTime;
    }

    public static TableProcessResult success(int tableIndex) {
        return new TableProcessResult(tableIndex, true, null, System.currentTimeMillis());
    }

    public static TableProcessResult success(int tableIndex, long processTime) {
        return new TableProcessResult(tableIndex, true, null, processTime);
    }

    public static TableProcessResult failure(int tableIndex, Exception error) {
        return new TableProcessResult(tableIndex, false, error, System.currentTimeMillis());
    }

    public static TableProcessResult failure(int tableIndex, Exception error, long processTime) {
        return new TableProcessResult(tableIndex, false, error, processTime);
    }

    public int getTableIndex() {
        return tableIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public Exception getError() {
        return error;
    }

    public long getProcessTime() {
        return processTime;
    }

    public boolean isFailure() {
        return !success;
    }
}