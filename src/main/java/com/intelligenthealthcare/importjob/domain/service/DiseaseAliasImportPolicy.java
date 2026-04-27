package com.intelligenthealthcare.importjob.domain.service;

import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.importjob.domain.model.ImportTableRow;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 疾病别名单行：解析为 {@link DiseaseAlias}，再与库中是否存在/重复配合决策。
 */
public final class DiseaseAliasImportPolicy {

    private DiseaseAliasImportPolicy() {}

    public static ParsedRow parse(ImportTableRow row) {
        if (row == null) {
            return ParsedRow.failure("行数据为空");
        }
        Map<String, String> v = row.getValues();
        if (v == null) {
            return ParsedRow.failure("行数据为空");
        }
        String code = trimToEmpty(v, "disease_code");
        String aliasName = trimToEmpty(v, "alias_name");
        if (code.isEmpty() || aliasName.isEmpty()) {
            return ParsedRow.failure("缺少 disease_code 或 alias_name");
        }
        DiseaseAlias ready =
                DiseaseAlias.builder()
                        .diseaseCode(code)
                        .aliasName(aliasName)
                        .aliasType(trimToNull(v, "alias_type"))
                        .source(trimToNull(v, "source"))
                        .build();
        return ParsedRow.ok(ready);
    }

    public static ImportLineProcessResult evaluate(
            DiseaseAlias rowAsEntity, boolean diseaseExists, boolean aliasAlreadyExists) {
        if (rowAsEntity == null) {
            return ImportLineProcessResult.fail("行数据为空");
        }
        if (!diseaseExists) {
            return ImportLineProcessResult.fail("疾病编码不存在：" + rowAsEntity.getDiseaseCode());
        }
        if (aliasAlreadyExists) {
            return ImportLineProcessResult.skipAlias();
        }
        return ImportLineProcessResult.insertAlias(rowAsEntity);
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

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ParsedRow {
        private final String errorMessage;
        private final DiseaseAlias alias;

        public static ParsedRow ok(DiseaseAlias alias) {
            return new ParsedRow(null, alias);
        }

        public static ParsedRow failure(String message) {
            return new ParsedRow(message, null);
        }

        public boolean isOk() {
            return errorMessage == null && alias != null;
        }
    }
}
