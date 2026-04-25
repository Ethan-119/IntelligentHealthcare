package com.intelligenthealthcare.importjob.api.dto;

import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportFailureLogResponse {
    long id;
    long jobId;
    int rowNumber;
    String rawContent;
    String errorMessage;
    LocalDateTime createTime;

    public static ImportFailureLogResponse fromEntity(ImportFailureLog e) {
        if (e == null) {
            return null;
        }
        return ImportFailureLogResponse.builder()
                .id(e.getId())
                .jobId(e.getJobId())
                .rowNumber(e.getRowNumber() != null ? e.getRowNumber() : 0)
                .rawContent(e.getRawContent())
                .errorMessage(e.getErrorMessage())
                .createTime(e.getCreateTime())
                .build();
    }

    public static List<ImportFailureLogResponse> fromList(List<ImportFailureLog> list) {
        List<ImportFailureLogResponse> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(fromEntity(list.get(i)));
        }
        return out;
    }
}
