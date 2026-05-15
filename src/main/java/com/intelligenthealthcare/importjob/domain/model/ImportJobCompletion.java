package com.intelligenthealthcare.importjob.domain.model;

import lombok.Value;

/**
 * 一次导入任务在领域侧计算出的终态与摘要文案。
 */
@Value
public class ImportJobCompletion {

    String status;
    String message;
}
