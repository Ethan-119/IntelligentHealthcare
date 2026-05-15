package com.intelligenthealthcare.importjob.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员确认某条"待审核"已处理时提交。
 */
@Data
public class ResolveImportReviewRequest {

    @NotBlank
    private String resolutionNote;
}
