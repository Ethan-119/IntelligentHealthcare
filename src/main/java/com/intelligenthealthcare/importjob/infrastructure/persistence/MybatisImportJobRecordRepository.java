package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.intelligenthealthcare.importjob.domain.ImportJobStatuses;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import com.intelligenthealthcare.importjob.domain.repository.ImportJobRecordRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisImportJobRecordRepository implements ImportJobRecordRepository {

    private final ImportJobRecordMapper importJobRecordMapper;

    public MybatisImportJobRecordRepository(ImportJobRecordMapper importJobRecordMapper) {
        this.importJobRecordMapper = importJobRecordMapper;
    }

    @Override
    public void insert(ImportJobRecord job) {
        importJobRecordMapper.insert(job);
    }

    @Override
    public void updateOnCompletion(
            long jobId,
            String status,
            int successCount,
            int failureCount,
            int reviewCount,
            int autoMappedCount,
            String message) {
        LambdaUpdateWrapper<ImportJobRecord> u = new LambdaUpdateWrapper<>();
        u.eq(ImportJobRecord::getId, jobId);
        u.set(ImportJobRecord::getStatus, status);
        u.set(ImportJobRecord::getSuccessCount, successCount);
        u.set(ImportJobRecord::getFailureCount, failureCount);
        u.set(ImportJobRecord::getReviewCount, reviewCount);
        u.set(ImportJobRecord::getAutoMappedCount, autoMappedCount);
        u.set(ImportJobRecord::getMessage, message);
        importJobRecordMapper.update(null, u);
    }

    @Override
    public void updateAsFailed(long jobId, String message) {
        LambdaUpdateWrapper<ImportJobRecord> u = new LambdaUpdateWrapper<>();
        u.eq(ImportJobRecord::getId, jobId);
        u.set(ImportJobRecord::getStatus, ImportJobStatuses.FAILED);
        u.set(ImportJobRecord::getMessage, message);
        importJobRecordMapper.update(null, u);
    }

    @Override
    public Optional<ImportJobRecord> findById(long id) {
        ImportJobRecord r = importJobRecordMapper.selectById(id);
        if (r == null) {
            return Optional.empty();
        }
        return Optional.of(r);
    }

    @Override
    public List<ImportJobRecord> listRecent(int maxCount) {
        LambdaQueryWrapper<ImportJobRecord> w = new LambdaQueryWrapper<>();
        w.orderByDesc(ImportJobRecord::getId);
        w.last("LIMIT " + maxCount);
        return importJobRecordMapper.selectList(w);
    }
}
