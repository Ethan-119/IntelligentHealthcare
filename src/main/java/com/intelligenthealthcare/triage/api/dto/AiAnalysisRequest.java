package com.intelligenthealthcare.triage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiAnalysisRequest {

    private String sessionId;

    @NotBlank(message = "症状描述不能为空")
    private String content;

    @Size(max = 5, message = "最多上传 5 张图片")
    private List<String> images;

    private Double latitude;
    private Double longitude;
}
