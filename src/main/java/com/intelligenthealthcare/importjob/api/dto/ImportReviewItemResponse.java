package com.intelligenthealthcare.importjob.api.dto;

import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportReviewItemResponse {
    long id;
    long jobId;
    String datasetType;
    String itemKey;
    String issueType;
    String rawContent;
    String suggestion;
    boolean resolved;
    String resolutionNote;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ImportReviewItemResponse fromEntity(ImportReviewItem e) {
        if (e == null) {
            return null;
        }
        return ImportReviewItemResponse.builder()
                .id(e.getId())
                .jobId(e.getJobId())
                .datasetType(e.getDatasetType())
                .itemKey(e.getItemKey())
                .issueType(e.getIssueType())
                .rawContent(e.getRawContent())
                .suggestion(e.getSuggestion())
                .resolved(e.getResolved() != null && e.getResolved() == 1)
                .resolutionNote(e.getResolutionNote())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }

    public static List<ImportReviewItemResponse> fromList(List<ImportReviewItem> list) {
        List<ImportReviewItemResponse> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(fromEntity(list.get(i)));
        }
        return out;
    }
}
