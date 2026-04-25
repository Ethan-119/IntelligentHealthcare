package com.intelligenthealthcare.importjob.domain.service;

import com.intelligenthealthcare.importjob.domain.model.DiseaseAliasImportLine;
import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;

/**
 * 疾病别名行：需疾病已存在；别名与库中重复时跳过。
 */
public final class DiseaseAliasImportPolicy {

    private DiseaseAliasImportPolicy() {}

    public static ImportLineProcessResult evaluate(
            DiseaseAliasImportLine line, boolean diseaseExists, boolean aliasAlreadyExists) {
        if (line == null) {
            return ImportLineProcessResult.fail("行数据为空");
        }
        if (!diseaseExists) {
            return ImportLineProcessResult.fail("疾病编码不存在：" + line.getDiseaseCode());
        }
        if (aliasAlreadyExists) {
            return ImportLineProcessResult.skipAlias();
        }
        return ImportLineProcessResult.insertAlias(line.toNewEntity());
    }
}
