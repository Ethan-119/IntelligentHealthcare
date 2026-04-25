package com.intelligenthealthcare.importjob.domain.model;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import java.util.Map;

/**
 * 疾病别名表导入行。
 */
public final class DiseaseAliasImportLine {

    private final String diseaseCode;
    private final String aliasName;
    private final String aliasType;
    private final String source;

    private DiseaseAliasImportLine(String diseaseCode, String aliasName, String aliasType, String source) {
        this.diseaseCode = diseaseCode;
        this.aliasName = aliasName;
        this.aliasType = aliasType;
        this.source = source;
    }

    public String getDiseaseCode() {
        return diseaseCode;
    }

    public String getAliasName() {
        return aliasName;
    }

    public DiseaseAlias toNewEntity() {
        return DiseaseAlias.builder()
                .diseaseCode(diseaseCode)
                .aliasName(aliasName)
                .aliasType(aliasType)
                .source(source)
                .build();
    }

    public static DiseaseAliasParseResult parse(ImportTableRow row) {
        if (row == null) {
            return DiseaseAliasParseResult.failure("行数据为空");
        }
        Map<String, String> v = row.getValues();
        if (v == null) {
            return DiseaseAliasParseResult.failure("行数据为空");
        }
        String code = trimToEmpty(v, "disease_code");
        String aliasName = trimToEmpty(v, "alias_name");
        if (code.isEmpty() || aliasName.isEmpty()) {
            return DiseaseAliasParseResult.failure("缺少 disease_code 或 alias_name");
        }
        return DiseaseAliasParseResult.ok(
                new DiseaseAliasImportLine(code, aliasName, trimToNull(v, "alias_type"), trimToNull(v, "source")));
    }

    private static String trimToEmpty(Map<String, String> m, String key) {
        if (m == null) {
            return "";
        }
        String s = m.get(key);
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    private static String trimToNull(Map<String, String> m, String key) {
        String t = trimToEmpty(m, key);
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}
