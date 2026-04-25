package com.intelligenthealthcare.importjob.domain.model;

import com.intelligenthealthcare.importjob.domain.DatasetTypes;
import com.intelligenthealthcare.importjob.domain.ReviewIssueTypes;

/**
 * 待人工审核的草稿，可转为 {@link ImportReviewItem} 持久化。
 */
public final class ImportReviewDraft {

    private final String datasetType;
    private final String itemKey;
    private final String issueType;
    private final String rawContent;
    private final String suggestion;

    public ImportReviewDraft(
            String datasetType, String itemKey, String issueType, String rawContent, String suggestion) {
        this.datasetType = datasetType;
        this.itemKey = itemKey;
        this.issueType = issueType;
        this.rawContent = rawContent;
        this.suggestion = ImportTextLimits.truncateMessage(suggestion, ImportTextLimits.SUGGESTION);
    }

    public static ImportReviewDraft duplicateDiseaseCode(String code, String rawLine) {
        return new ImportReviewDraft(
                DatasetTypes.DISEASE_MASTER,
                code,
                ReviewIssueTypes.DUPLICATE_DISEASE_CODE,
                rawLine,
                "已存在同编码(" + code + ")的疾病，但名称与库中不一致，请人工处理。");
    }

    public String getDatasetType() {
        return datasetType;
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getRawContent() {
        return rawContent;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
