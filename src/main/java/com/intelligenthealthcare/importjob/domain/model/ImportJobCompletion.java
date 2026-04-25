package com.intelligenthealthcare.importjob.domain.model;

/**
 * 一次导入任务在领域侧计算出的终态与摘要文案。
 */
public final class ImportJobCompletion {

    private final String status;
    private final String message;

    public ImportJobCompletion(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
