package com.intelligenthealthcare.importjob.domain.model;

/**
 * 疾病主表行解析：成功携 {@link DiseaseMasterImportLine}，失败携消息。
 */
public final class DiseaseMasterParseResult {

    private final boolean ok;
    private final String errorMessage;
    private final DiseaseMasterImportLine line;

    private DiseaseMasterParseResult(boolean ok, String errorMessage, DiseaseMasterImportLine line) {
        this.ok = ok;
        this.errorMessage = errorMessage;
        this.line = line;
    }

    public static DiseaseMasterParseResult ok(DiseaseMasterImportLine line) {
        return new DiseaseMasterParseResult(true, null, line);
    }

    public static DiseaseMasterParseResult failure(String message) {
        return new DiseaseMasterParseResult(false, message, null);
    }

    public boolean isOk() {
        return ok;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DiseaseMasterImportLine getLine() {
        return line;
    }
}
