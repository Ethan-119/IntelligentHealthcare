package com.intelligenthealthcare.triage.application.dto;

import java.time.LocalDateTime;

public record AiSessionSummary(
        String sessionId,
        String title,
        Integer askRound,
        LocalDateTime updateTime) {
}
