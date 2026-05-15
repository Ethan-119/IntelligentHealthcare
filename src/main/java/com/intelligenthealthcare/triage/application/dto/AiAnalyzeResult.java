package com.intelligenthealthcare.triage.application.dto;

public record AiAnalyzeResult(String sessionId, String result, String imageAnalysis) {
    public AiAnalyzeResult(String sessionId, String result) {
        this(sessionId, result, null);
    }
}
