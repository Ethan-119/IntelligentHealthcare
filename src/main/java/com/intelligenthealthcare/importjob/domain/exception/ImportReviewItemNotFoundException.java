package com.intelligenthealthcare.importjob.domain.exception;

/**
 * 审核项不存在或已删除。
 */
public class ImportReviewItemNotFoundException extends RuntimeException {

    public ImportReviewItemNotFoundException() {
        super("待审核项不存在");
    }
}
