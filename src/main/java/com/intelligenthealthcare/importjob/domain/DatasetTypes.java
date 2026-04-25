package com.intelligenthealthcare.importjob.domain;

import java.util.Locale;
import java.util.Set;

/**
 * 导入数据类型；与业务表、Excel/CSV 列说明对应。
 */
public final class DatasetTypes {

    private DatasetTypes() {}

    /** 疾病主表 {@code disease_master} */
    public static final String DISEASE_MASTER = "DISEASE_MASTER";
    /** 疾病别名 {@code disease_alias} */
    public static final String DISEASE_ALIAS = "DISEASE_ALIAS";

    private static final Set<String> ALL = Set.of(DISEASE_MASTER, DISEASE_ALIAS);

    public static boolean isValid(String datasetType) {
        if (datasetType == null) {
            return false;
        }
        return ALL.contains(datasetType.trim().toUpperCase(Locale.ROOT));
    }
}
