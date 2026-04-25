package com.intelligenthealthcare.importjob.domain;

import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import com.intelligenthealthcare.importjob.domain.model.ImportTextLimits;

/**
 * 新建导入任务、文件名安全处理等，属领域侧工厂，非应用服务。
 */
public final class ImportJobFactory {

    private ImportJobFactory() {}

    public static ImportJobRecord newRunningJob(String datasetType, String fileName) {
        return ImportJobRecord.builder()
                .datasetType(datasetType)
                .fileName(safeFileName(fileName))
                .status(ImportJobStatuses.RUNNING)
                .successCount(0)
                .failureCount(0)
                .reviewCount(0)
                .autoMappedCount(0)
                .message(null)
                .build();
    }

    public static String safeFileName(String name) {
        if (name == null) {
            return null;
        }
        return ImportTextLimits.truncateMessage(name, ImportTextLimits.FILE_NAME);
    }
}
