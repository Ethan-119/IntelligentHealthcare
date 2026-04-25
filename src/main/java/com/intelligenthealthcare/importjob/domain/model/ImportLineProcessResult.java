package com.intelligenthealthcare.importjob.domain.model;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;

/**
 * 单行在领域规则下处理后的结果，供应用层落库。不包含持久化，仅描述要做什么。
 */
public final class ImportLineProcessResult {

    public enum Outcome {
        FAIL,
        NEED_REVIEW,
        INSERT_DISEASE_MASTER,
        SKIP_DISEASE_MASTER,
        INSERT_DISEASE_ALIAS,
        SKIP_DISEASE_ALIAS
    }

    private final Outcome outcome;
    private final String failureMessage;
    private final ImportReviewDraft review;
    private final DiseaseMaster masterToInsert;
    private final DiseaseAlias aliasToInsert;

    private ImportLineProcessResult(
            Outcome outcome,
            String failureMessage,
            ImportReviewDraft review,
            DiseaseMaster masterToInsert,
            DiseaseAlias aliasToInsert) {
        this.outcome = outcome;
        this.failureMessage = failureMessage;
        this.review = review;
        this.masterToInsert = masterToInsert;
        this.aliasToInsert = aliasToInsert;
    }

    public static ImportLineProcessResult fail(String message) {
        return new ImportLineProcessResult(Outcome.FAIL, message, null, null, null);
    }

    public static ImportLineProcessResult needReview(ImportReviewDraft draft) {
        return new ImportLineProcessResult(Outcome.NEED_REVIEW, null, draft, null, null);
    }

    public static ImportLineProcessResult insertMaster(DiseaseMaster master) {
        return new ImportLineProcessResult(Outcome.INSERT_DISEASE_MASTER, null, null, master, null);
    }

    public static ImportLineProcessResult skipMaster() {
        return new ImportLineProcessResult(Outcome.SKIP_DISEASE_MASTER, null, null, null, null);
    }

    public static ImportLineProcessResult insertAlias(DiseaseAlias alias) {
        return new ImportLineProcessResult(Outcome.INSERT_DISEASE_ALIAS, null, null, null, alias);
    }

    public static ImportLineProcessResult skipAlias() {
        return new ImportLineProcessResult(Outcome.SKIP_DISEASE_ALIAS, null, null, null, null);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public ImportReviewDraft getReview() {
        return review;
    }

    public DiseaseMaster getMasterToInsert() {
        return masterToInsert;
    }

    public DiseaseAlias getAliasToInsert() {
        return aliasToInsert;
    }
}
