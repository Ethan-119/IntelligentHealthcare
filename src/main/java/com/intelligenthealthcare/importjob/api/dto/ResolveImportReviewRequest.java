package com.intelligenthealthcare.importjob.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员确认某条“待审核”已处理时提交。
 */
public class ResolveImportReviewRequest {

    @NotBlank
    private String resolutionNote;

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }
}
