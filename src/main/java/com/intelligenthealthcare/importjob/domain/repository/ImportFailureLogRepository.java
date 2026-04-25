package com.intelligenthealthcare.importjob.domain.repository;

import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import java.util.List;

/**
 * 导入失败行（import_failure_log）仓储。
 */
public interface ImportFailureLogRepository {

    void save(ImportFailureLog log);

    List<ImportFailureLog> listByJobIdOrderById(long jobId);
}
