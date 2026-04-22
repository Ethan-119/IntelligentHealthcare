package com.intelligenthealthcare.triage.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalysisResponse {

    private String result;
}
