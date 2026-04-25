package com.intelligenthealthcare.importjob.domain.service;

import com.intelligenthealthcare.importjob.domain.model.DiseaseMasterImportLine;
import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewDraft;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;

/**
 * 疾病主表行与库内现状的去重/审核领域规则，无基础设施依赖。
 */
public final class DiseaseMasterImportPolicy {

    private DiseaseMasterImportPolicy() {}

    /**
     * 根据是否已存在同编码、名称是否一致，决定本行是插入、跳过或进入待办。{@code rawLine} 仅用于待办中展示源行原文。
     */
    public static ImportLineProcessResult evaluate(
            DiseaseMasterImportLine line, DiseaseMaster existing, String rawLine) {
        if (line == null) {
            return ImportLineProcessResult.fail("行数据为空");
        }
        if (existing == null) {
            return ImportLineProcessResult.insertMaster(line.toNewDiseaseEntity());
        }
        if (line.sameNameAs(existing)) {
            return ImportLineProcessResult.skipMaster();
        }
        return ImportLineProcessResult.needReview(
                ImportReviewDraft.duplicateDiseaseCode(line.getDiseaseCode(), rawLine == null ? "" : rawLine));
    }
}
