package com.intelligenthealthcare.importjob.domain.model;

/**
 * 与库字段长度等对齐的文本截断上界，供领域层与持久化前使用。
 */
public final class ImportTextLimits {

    private ImportTextLimits() {}

    public static final int ERROR_MESSAGE = 500;
    public static final int FILE_NAME = 255;
    public static final int SUGGESTION = 500;

    public static String truncateMessage(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
