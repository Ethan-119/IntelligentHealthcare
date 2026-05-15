package com.intelligenthealthcare.importjob.domain.model;

import com.intelligenthealthcare.importjob.domain.ImportJobStatuses;
import lombok.Getter;

/**
 * 单次导入在内存中累计的计数，由 {@link ImportLineProcessResult} 驱动更新。
 * Lombok @Value 不适用：此类有 apply/toCompletion 等 mutation 方法。
 */
@Getter
public final class ImportJobProgress {

    private int successCount;
    private int failureCount;
    private int reviewCount;
    private int autoMappedCount;

    public void apply(ImportLineProcessResult r) {
        if (r == null) {
            return;
        }
        switch (r.getOutcome()) {
            case FAIL:
                failureCount++;
                break;
            case NEED_REVIEW:
                reviewCount++;
                break;
            case INSERT_DISEASE_MASTER:
            case INSERT_DISEASE_ALIAS:
                successCount++;
                break;
            case SKIP_DISEASE_MASTER:
            case SKIP_DISEASE_ALIAS:
                successCount++;
                autoMappedCount++;
                break;
            default:
                break;
        }
    }

    public ImportJobCompletion toCompletion() {
        String status;
        if (failureCount > 0 || reviewCount > 0) {
            status = ImportJobStatuses.PARTIAL_SUCCESS;
        } else {
            status = ImportJobStatuses.SUCCESS;
        }
        String message =
                "完成：成功 "
                        + successCount
                        + "，失败 "
                        + failureCount
                        + "，待审核 "
                        + reviewCount
                        + "，去重/跳过 "
                        + autoMappedCount;
        return new ImportJobCompletion(status, message);
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getAutoMappedCount() {
        return autoMappedCount;
    }
}
