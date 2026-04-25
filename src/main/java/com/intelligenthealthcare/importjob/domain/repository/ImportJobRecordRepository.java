package com.intelligenthealthcare.importjob.domain.repository;

import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import java.util.List;
import java.util.Optional;

/**
 * 导入任务聚合（import_job_record）仓储接口。应用层只依赖本接口，不直接依赖 MyBatis。
 */
public interface ImportJobRecordRepository {

    void insert(ImportJobRecord job);

    void updateOnCompletion(
            long jobId,
            String status,
            int successCount,
            int failureCount,
            int reviewCount,
            int autoMappedCount,
            String message);

    void updateAsFailed(long jobId, String message);

    Optional<ImportJobRecord> findById(long id);

    List<ImportJobRecord> listRecent(int maxCount);
}
