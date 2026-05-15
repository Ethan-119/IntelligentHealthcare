package com.intelligenthealthcare.triage.application.dto;

import java.util.List;

public record AiSessionConversation(
        String sessionId,
        List<AiSessionTurn> turns) {
}
