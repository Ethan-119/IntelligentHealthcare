package com.intelligenthealthcare.importjob.domain.service;

import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewDraft;
import com.intelligenthealthcare.importjob.domain.model.ImportTableRow;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 疾病主表导入：从行解析为 {@link DiseaseMaster}，再与库内数据按规则去重/待办；不另建 importjob 层「行 DTO」。
 */
public final class DiseaseMasterImportPolicy {

    private DiseaseMasterImportPolicy() {}

    /**
     * 从表行解析为待入库/待比较的 {@link DiseaseMaster}；失败时 errorMessage 非空。
     */
    public static ParsedRow parse(ImportTableRow row) {
        if (row == null) {
            return ParsedRow.failure("行数据为空");
        }
        Map<String, String> v = row.getValues();
        if (v == null) {
            return ParsedRow.failure("行数据为空");
        }
        String code = trimToEmpty(v, "disease_code");
        String name = trimToEmpty(v, "disease_name");
        if (code.isEmpty() || name.isEmpty()) {
            return ParsedRow.failure("缺少 disease_code 或 disease_name");
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
                return ParsedRow.failure("age_min 不是合法整数");
            }
        }
        Integer ageMax = null;
        if (notBlank(v, "age_max")) {
            try {
                ageMax = Integer.parseInt(v.get("age_max").trim());
            } catch (NumberFormatException e) {
                return ParsedRow.failure("age_max 不是合法整数");
            }
        }
        DiseaseMaster ready =
                DiseaseMaster.builder()
                        .diseaseCode(code)
                        .diseaseName(name)
                        .aliasesJson(aliasesJson)
                        .symptomKeywords(symptomKeywords)
                        .genderRule(genderRule)
                        .ageMin(ageMin)
                        .ageMax(ageMax)
                        .ageGroup(ageGroup)
                        .urgencyLevel(urgency)
                        .reviewStatus(reviewSt)
                        .deleted(0)
                        .build();
        return ParsedRow.ok(ready);
    }

    /**
     * 根据是否已存在同编码、名称是否一致，决定本行是插入、跳过或进入待办。{@code rawLine} 仅用于待办中展示源行原文。
     */
    public static ImportLineProcessResult evaluate(
            DiseaseMaster rowAsEntity, DiseaseMaster existing, String rawLine) {
        if (rowAsEntity == null) {
            return ImportLineProcessResult.fail("行数据为空");
        }
        if (existing == null) {
            return ImportLineProcessResult.insertMaster(rowAsEntity);
        }
        if (sameDiseaseName(rowAsEntity, existing)) {
            return ImportLineProcessResult.skipMaster();
        }
        return ImportLineProcessResult.needReview(
                ImportReviewDraft.duplicateDiseaseCode(
                        rowAsEntity.getDiseaseCode(), rawLine == null ? "" : rawLine));
    }

    private static boolean sameDiseaseName(DiseaseMaster row, DiseaseMaster existing) {
        if (row == null || existing == null) {
            return false;
        }
        String a = row.getDiseaseName();
        String b = existing.getDiseaseName();
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
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

    /** 解析行结果，不单独成 domain.model 文件。 */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ParsedRow {
        private final String errorMessage;
        private final DiseaseMaster disease;

        public static ParsedRow ok(DiseaseMaster disease) {
            return new ParsedRow(null, disease);
        }

        public static ParsedRow failure(String message) {
            return new ParsedRow(message, null);
        }

        public boolean isOk() {
            return errorMessage == null && disease != null;
        }
    }
}
