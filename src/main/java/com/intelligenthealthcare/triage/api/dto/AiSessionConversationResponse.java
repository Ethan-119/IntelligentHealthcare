package com.intelligenthealthcare.triage.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiSessionConversationResponse {
    private String sessionId;
    private List<AiSessionTurnResponse> turns;
}
