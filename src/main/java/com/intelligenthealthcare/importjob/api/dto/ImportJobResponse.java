package com.intelligenthealthcare.importjob.api.dto;

import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportJobResponse {
    long id;
    String datasetType;
    String fileName;
    String status;
    int successCount;
    int failureCount;
    int reviewCount;
    int autoMappedCount;
    String message;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ImportJobResponse fromEntity(ImportJobRecord e) {
        if (e == null) {
            return null;
        }
        return ImportJobResponse.builder()
                .id(e.getId())
                .datasetType(e.getDatasetType())
                .fileName(e.getFileName())
                .status(e.getStatus())
                .successCount(zeroIfNull(e.getSuccessCount()))
                .failureCount(zeroIfNull(e.getFailureCount()))
                .reviewCount(zeroIfNull(e.getReviewCount()))
                .autoMappedCount(zeroIfNull(e.getAutoMappedCount()))
                .message(e.getMessage())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }

    private static int zeroIfNull(Integer v) {
        if (v == null) {
            return 0;
        }
        return v;
    }
}
