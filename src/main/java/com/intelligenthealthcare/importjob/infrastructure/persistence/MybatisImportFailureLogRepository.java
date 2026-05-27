package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import com.intelligenthealthcare.importjob.domain.repository.ImportFailureLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link ImportFailureLogRepository} 的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MybatisImportFailureLogRepository implements ImportFailureLogRepository {

    private final ImportFailureLogMapper importFailureLogMapper;

    @Override
    public void save(ImportFailureLog log) {
        importFailureLogMapper.insert(log);
    }

    @Override
    public List<ImportFailureLog> listByJobIdOrderById(long jobId) {
        LambdaQueryWrapper<ImportFailureLog> w = new LambdaQueryWrapper<>();
        w.eq(ImportFailureLog::getJobId, jobId);
        w.orderByAsc(ImportFailureLog::getId);
        return importFailureLogMapper.selectList(w);
    }
}
