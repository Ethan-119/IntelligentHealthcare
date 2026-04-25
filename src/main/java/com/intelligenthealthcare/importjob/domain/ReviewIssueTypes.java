package com.intelligenthealthcare.importjob.domain;

/**
 * 写入 {@code import_review_item.issue_type} 的常用取值。
 */
public final class ReviewIssueTypes {

    private ReviewIssueTypes() {}

    public static final String DUPLICATE_DISEASE_CODE = "DUPLICATE_DISEASE_CODE";
    public static final String NEEDS_MANUAL_REVIEW = "NEEDS_MANUAL_REVIEW";
}
