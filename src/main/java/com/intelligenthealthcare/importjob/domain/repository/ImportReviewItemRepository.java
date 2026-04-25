package com.intelligenthealthcare.importjob.domain.repository;

import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import java.util.List;
import java.util.Optional;

/**
 * 导入待审核项（import_review_item）仓储。
 */
public interface ImportReviewItemRepository {

    void save(ImportReviewItem item);

    Optional<ImportReviewItem> findById(long id);

    void update(ImportReviewItem item);

    List<ImportReviewItem> listByJobId(long jobId, Boolean resolved);
}
