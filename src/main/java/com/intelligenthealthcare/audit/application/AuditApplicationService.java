package com.intelligenthealthcare.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligenthealthcare.audit.domain.model.AiRecallAuditLog;
import com.intelligenthealthcare.audit.domain.repository.AiRecallAuditLogRepository;
import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.Patient;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditApplicationService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final Pattern DISEASE_CODE_PATTERN = Pattern.compile("DIS_\\d+");

    private final AiRecallAuditLogRepository aiRecallAuditLogRepository;
    private final ObjectMapper objectMapper;

    public void recordAiAnalyzeSuccess(String content, List<String> images, String result, Patient patient, String sessionId) {
        List<String> suggestedCodes = extractDiseaseCodes(result);
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .gender(resolveGender(patient))
                .age(resolveAge(patient))
                .ageGroup(resolveAgeGroup(patient))
                .eligibleDiseaseCount(suggestedCodes.size())
                .ruleCandidateCodesJson(toImageSummaryJson(images))
                .suggestedCodesJson(toJson(suggestedCodes))
                .status("SUCCESS")
                .message(trimText(appendSessionHint(result, sessionId)))
                .build();
        saveQuietly(entity);
    }

    public void recordAiAnalyzeFailed(String content, List<String> images, String errorMessage, Patient patient, String sessionId) {
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .gender(resolveGender(patient))
                .age(resolveAge(patient))
                .ageGroup(resolveAgeGroup(patient))
                .eligibleDiseaseCount(0)
                .ruleCandidateCodesJson(toImageSummaryJson(images))
                .suggestedCodesJson("[]")
                .status("FAILED")
                .message(trimText(appendSessionHint(errorMessage, sessionId)))
                .build();
        saveQuietly(entity);
    }

    public void recordEmergencyAlert(String symptomSummary, String reason) {
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(symptomSummary))
                .status("EMERGENCY_ALERT")
                .message(trimText(reason))
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

    private String toImageSummaryJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        List<String> summary = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String raw = images.get(i);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String text = raw.trim();
            int len = text.length();
            summary.add(text.substring(0, Math.min(80, len)) + "...(len=" + len + ")");
        }
        return toJson(summary);
    }

    private List<String> extractDiseaseCodes(String text) {
        List<String> codes = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return codes;
        }
        Matcher matcher = DISEASE_CODE_PATTERN.matcher(text);
        while (matcher.find()) {
            String code = matcher.group();
            boolean exists = false;
            for (int i = 0; i < codes.size(); i++) {
                if (codes.get(i).equals(code)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                codes.add(code);
            }
        }
        return codes;
    }

    private Integer resolveAge(Patient patient) {
        return patient == null ? null : patient.getPatientAge();
    }

    private String resolveGender(Patient patient) {
        if (patient == null || patient.getPatientGender() == null) {
            return Gender.UNKNOWN.getCode();
        }
        return patient.getPatientGender().getCode();
    }

    private String resolveAgeGroup(Patient patient) {
        Integer age = resolveAge(patient);
        if (age == null) {
            return "unknown";
        }
        if (age < 18) {
            return "child";
        }
        if (age >= 60) {
            return "elder";
        }
        return "adult";
    }

    private String appendSessionHint(String message, String sessionId) {
        String text = trimText(message);
        if (!StringUtils.hasText(sessionId)) {
            return text;
        }
        String base = StringUtils.hasText(text) ? text : "AI_ANALYZE";
        return trimText(base + " | sessionId=" + sessionId.trim());
    }
}
