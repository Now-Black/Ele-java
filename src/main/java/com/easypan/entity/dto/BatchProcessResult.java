package com.easypan.entity.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量处理结果
 */
public class BatchProcessResult {
    private List<TableProcessResult> results;
    private int totalCount;
    private int successCount;
    private int failureCount;

    public BatchProcessResult(List<TableProcessResult> results) {
        this.results = results;
        this.totalCount = results.size();
        this.successCount = (int) results.stream().filter(TableProcessResult::isSuccess).count();
        this.failureCount = totalCount - successCount;
    }

    public boolean isAllSuccess() {
        return failureCount == 0;
    }

    public boolean hasFailures() {
        return failureCount > 0;
    }

    public List<TableProcessResult> getSuccesses() {
        return results.stream()
                .filter(TableProcessResult::isSuccess)
                .collect(Collectors.toList());
    }

    public List<TableProcessResult> getFailures() {
        return results.stream()
                .filter(TableProcessResult::isFailure)
                .collect(Collectors.toList());
    }

    public List<TableProcessResult> getResults() {
        return results;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public double getSuccessRate() {
        return totalCount == 0 ? 0.0 : (double) successCount / totalCount;
    }
}