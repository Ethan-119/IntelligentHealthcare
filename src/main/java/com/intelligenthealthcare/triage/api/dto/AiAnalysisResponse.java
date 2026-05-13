package com.intelligenthealthcare.triage.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalysisResponse {

    private String sessionId;
    private String result;
    private String imageAnalysis;
}
