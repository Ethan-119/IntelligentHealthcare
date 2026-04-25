package com.intelligenthealthcare.importjob.domain.model;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.util.Map;

/**
 * 疾病主表导入行：从表头列解析出的领域值，负责校验与构建待入库实体。
 */
public final class DiseaseMasterImportLine {

    private final String diseaseCode;
    private final String diseaseName;
    private final String symptomKeywords;
    private final String genderRule;
    private final Integer ageMin;
    private final Integer ageMax;
    private final String ageGroup;
    private final String urgencyLevel;
    private final String reviewStatus;
    private final String aliasesJson;

    private DiseaseMasterImportLine(
            String diseaseCode,
            String diseaseName,
            String symptomKeywords,
            String genderRule,
            Integer ageMin,
            Integer ageMax,
            String ageGroup,
            String urgencyLevel,
            String reviewStatus,
            String aliasesJson) {
        this.diseaseCode = diseaseCode;
        this.diseaseName = diseaseName;
        this.symptomKeywords = symptomKeywords;
        this.genderRule = genderRule;
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.ageGroup = ageGroup;
        this.urgencyLevel = urgencyLevel;
        this.reviewStatus = reviewStatus;
        this.aliasesJson = aliasesJson;
    }

    public String getDiseaseCode() {
        return diseaseCode;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public DiseaseMaster toNewDiseaseEntity() {
        return DiseaseMaster.builder()
                .diseaseCode(diseaseCode)
                .diseaseName(diseaseName)
                .aliasesJson(aliasesJson)
                .symptomKeywords(symptomKeywords)
                .genderRule(genderRule)
                .ageMin(ageMin)
                .ageMax(ageMax)
                .ageGroup(ageGroup)
                .urgencyLevel(urgencyLevel)
                .reviewStatus(reviewStatus)
                .deleted(0)
                .build();
    }

    /**
     * 解析表头为 disease_code、disease_name 等列的一行；失败时返回带错误信息的结果。
     */
    public static DiseaseMasterParseResult parse(ImportTableRow row) {
        if (row == null) {
            return DiseaseMasterParseResult.failure("行数据为空");
        }
        Map<String, String> v = row.getValues();
        if (v == null) {
            return DiseaseMasterParseResult.failure("行数据为空");
        }
        String code = trimToEmpty(v, "disease_code");
        String name = trimToEmpty(v, "disease_name");
        if (code.isEmpty() || name.isEmpty()) {
            return DiseaseMasterParseResult.failure("缺少 disease_code 或 disease_name");
        }
        String symptomKeywords = trimToNull(v, "symptom_keywords");
        String genderRule = trimToDefault(v, "gender_rule", "all");
        String ageGroup = trimToNull(v, "age_group");
        String urgency = trimToDefault(v, "urgency_level", "medium");
        String reviewSt = trimToDefault(v, "review_status", "approved");
        String aliasesJson = trimToNull(v, "aliases_json");

        Integer ageMin = null;
        if (notBlank(v, "age_min")) {
            try {
                ageMin = Integer.parseInt(v.get("age_min").trim());
            } catch (NumberFormatException e) {
                return DiseaseMasterParseResult.failure("age_min 不是合法整数");
            }
        }
        Integer ageMax = null;
        if (notBlank(v, "age_max")) {
            try {
                ageMax = Integer.parseInt(v.get("age_max").trim());
            } catch (NumberFormatException e) {
                return DiseaseMasterParseResult.failure("age_max 不是合法整数");
            }
        }
        return DiseaseMasterParseResult.ok(
                new DiseaseMasterImportLine(
                        code,
                        name,
                        symptomKeywords,
                        genderRule,
                        ageMin,
                        ageMax,
                        ageGroup,
                        urgency,
                        reviewSt,
                        aliasesJson));
    }

    public boolean sameNameAs(DiseaseMaster existing) {
        if (existing == null) {
            return false;
        }
        String n = existing.getDiseaseName();
        if (n == null) {
            return false;
        }
        return n.equals(diseaseName);
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

    private static String trimToDefault(Map<String, String> m, String key, String def) {
        String t = trimToEmpty(m, key);
        if (t.isEmpty()) {
            return def;
        }
        return t;
    }

    private static boolean notBlank(Map<String, String> m, String key) {
        String s = m.get(key);
        return s != null && !s.trim().isEmpty();
    }
}
