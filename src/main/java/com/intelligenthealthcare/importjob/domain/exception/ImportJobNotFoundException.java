package com.intelligenthealthcare.importjob.domain.exception;

/**
 * 导入任务不存在。
 */
public class ImportJobNotFoundException extends RuntimeException {

    public ImportJobNotFoundException() {
        super("导入任务不存在");
    }
}
