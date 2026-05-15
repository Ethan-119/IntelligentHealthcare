package com.intelligenthealthcare.triage.application.dto;

import java.time.LocalDateTime;

public record AiSessionTurn(
        Integer turnNo,
        String userMessage,
        String replyText,
        LocalDateTime createTime) {
}
