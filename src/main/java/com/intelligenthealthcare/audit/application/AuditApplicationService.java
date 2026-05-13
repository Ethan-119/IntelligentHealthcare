package com.intelligenthealthcare.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligenthealthcare.audit.domain.model.AiRecallAuditLog;
import com.intelligenthealthcare.audit.domain.repository.AiRecallAuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditApplicationService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final AiRecallAuditLogRepository aiRecallAuditLogRepository;
    private final ObjectMapper objectMapper;

    public void recordAiAnalyzeSuccess(String content, List<String> images, String result) {
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .ruleCandidateCodesJson(toJson(images))
                .status("SUCCESS")
                .message(trimText(result))
                .build();
        saveQuietly(entity);
    }

    public void recordAiAnalyzeFailed(String content, List<String> images, String errorMessage) {
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .ruleCandidateCodesJson(toJson(images))
                .status("FAILED")
                .message(trimText(errorMessage))
                .build();
        saveQuietly(entity);
    }

    private void saveQuietly(AiRecallAuditLog entity) {
        try {
            aiRecallAuditLogRepository.save(entity);
        } catch (RuntimeException ex) {
            // 审计失败不影响主流程。
            log.warn("写入 AI 审计日志失败: {}", ex.getMessage());
        }
    }

    private String trimText(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String text = input.trim();
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_MESSAGE_LENGTH);
    }

    private String toJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
