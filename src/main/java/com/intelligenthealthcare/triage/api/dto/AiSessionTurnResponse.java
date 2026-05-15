package com.intelligenthealthcare.triage.api.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiSessionTurnResponse {
    private Integer turnNo;
    private String userMessage;
    private String replyText;
    private LocalDateTime createTime;
}
