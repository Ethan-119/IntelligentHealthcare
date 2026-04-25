package com.intelligenthealthcare.importjob.domain.model;

/**
 * 别名单行解析结果。
 */
public final class DiseaseAliasParseResult {

    private final boolean ok;
    private final String errorMessage;
    private final DiseaseAliasImportLine line;

    private DiseaseAliasParseResult(boolean ok, String errorMessage, DiseaseAliasImportLine line) {
        this.ok = ok;
        this.errorMessage = errorMessage;
        this.line = line;
    }

    public static DiseaseAliasParseResult ok(DiseaseAliasImportLine line) {
        return new DiseaseAliasParseResult(true, null, line);
    }

    public static DiseaseAliasParseResult failure(String message) {
        return new DiseaseAliasParseResult(false, message, null);
    }

    public boolean isOk() {
        return ok;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DiseaseAliasImportLine getLine() {
        return line;
    }
}
