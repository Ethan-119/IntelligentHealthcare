package com.intelligenthealthcare.triage.api.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiSessionSummaryResponse {
    private String sessionId;
    private String title;
    private Integer askRound;
    private LocalDateTime updateTime;
}
