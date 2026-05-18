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

    /**
     * 是否开启深度图片分析：
     * false（默认）= 快速模式，分析较少图片；
     * true = 深度模式，分析更多图片，耗时更长。
     */
    private Boolean deepImageAnalysis;
}
