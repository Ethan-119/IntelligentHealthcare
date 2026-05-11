package com.intelligenthealthcare.triage.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiAnalysisRequest {

    private String content;
    private List<String> images;
}
