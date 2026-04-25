package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import com.intelligenthealthcare.importjob.domain.repository.ImportReviewItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisImportReviewItemRepository implements ImportReviewItemRepository {

    private final ImportReviewItemMapper importReviewItemMapper;

    public MybatisImportReviewItemRepository(ImportReviewItemMapper importReviewItemMapper) {
        this.importReviewItemMapper = importReviewItemMapper;
    }

    @Override
    public void save(ImportReviewItem item) {
        importReviewItemMapper.insert(item);
    }

    @Override
    public Optional<ImportReviewItem> findById(long id) {
        ImportReviewItem r = importReviewItemMapper.selectById(id);
        if (r == null) {
            return Optional.empty();
        }
        return Optional.of(r);
    }

    @Override
    public void update(ImportReviewItem item) {
        importReviewItemMapper.updateById(item);
    }

    @Override
    public List<ImportReviewItem> listByJobId(long jobId, Boolean resolved) {
        LambdaQueryWrapper<ImportReviewItem> w = new LambdaQueryWrapper<>();
        w.eq(ImportReviewItem::getJobId, jobId);
        if (resolved != null) {
            w.eq(ImportReviewItem::getResolved, Boolean.TRUE.equals(resolved) ? 1 : 0);
        }
        w.orderByAsc(ImportReviewItem::getId);
        return importReviewItemMapper.selectList(w);
    }
}
