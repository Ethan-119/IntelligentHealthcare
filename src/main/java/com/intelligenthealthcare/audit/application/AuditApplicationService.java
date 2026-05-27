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

/**
 * 事中审计（记录每一次AI问诊的过程）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditApplicationService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    // 用于从文本中提取标准疾病编码的正则表达式，支持匹配 "DIS_数字" 或 "TEST_DIS_数字"
    private static final Pattern DISEASE_CODE_PATTERN = Pattern.compile("(?:TEST_)?(DIS_\\d+)");

    private final AiRecallAuditLogRepository aiRecallAuditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录 AI 分析成功的审计日志。
     * <p>
     * 当 AI 成功返回诊断建议时调用此方法。它会提取规则引擎推荐的疾病码和 AI 最终建议的疾病码，
     * 并记录患者的基本信息及会话上下文，状态标记为 "SUCCESS"。
     *
     * @param content      用户输入的症状描述或问诊内容
     * @param images       用户上传的图片列表（当前版本暂未持久化图片信息）
     * @param result       AI 模型返回的分析结果文本
     * @param patient      当前问诊的患者实体信息
     * @param sessionId    当前问诊的会话唯一标识
     * @param reactTrace   前端 React 传递的规则引擎调试轨迹（包含规则推荐的疾病码）
     */
    public void recordAiAnalyzeSuccess(
            String content,
            List<String> images,
            String result,
            Patient patient,
            String sessionId,
            String reactTrace) {
        List<String> ruleCandidateCodes = extractDiseaseCodes(reactTrace);
        List<String> suggestedCodes = extractDiseaseCodes(result);
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .gender(resolveGender(patient))
                .age(resolveAge(patient))
                .ageGroup(resolveAgeGroup(patient))
                .eligibleDiseaseCount(suggestedCodes.size())
                .ruleCandidateCodesJson(toJson(ruleCandidateCodes))
                .suggestedCodesJson(toJson(suggestedCodes))
                .status("SUCCESS")
                .message(trimText(appendSessionHint(result, sessionId)))
                .build();
        saveQuietly(entity);
    }

    /**
     * 记录 AI 分析失败的审计日志。
     * <p>
     * 当 AI 调用出错、超时或处理异常时调用。该方法会捕获具体的报错信息，并将状态标记为 "FAILED"，
     * 同时将建议的疾病数量置为 0，便于后续监控系统稳定性及排查故障。
     *
     * @param content      用户输入的症状描述或问诊内容
     * @param images       用户上传的图片列表
     * @param errorMessage AI 处理过程中抛出的具体错误信息
     * @param patient      当前问诊的患者实体信息
     * @param sessionId    当前问诊的会话唯一标识
     * @param reactTrace   前端 React 传递的规则引擎调试轨迹
     */
    public void recordAiAnalyzeFailed(
            String content,
            List<String> images,
            String errorMessage,
            Patient patient,
            String sessionId,
            String reactTrace) {
        List<String> ruleCandidateCodes = extractDiseaseCodes(reactTrace);
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(content))
                .gender(resolveGender(patient))
                .age(resolveAge(patient))
                .ageGroup(resolveAgeGroup(patient))
                .eligibleDiseaseCount(0)
                .ruleCandidateCodesJson(toJson(ruleCandidateCodes))
                .suggestedCodesJson("[]")
                .status("FAILED")
                .message(trimText(appendSessionHint(errorMessage, sessionId)))
                .build();
        saveQuietly(entity);
    }

    /**
     * 记录紧急预警事件的审计日志。
     * <p>
     * 当系统识别到用户描述中包含高危关键词（如自杀倾向、剧烈胸痛等）时触发。
     * 该方法将状态标记为 "EMERGENCY_ALERT"，并单独记录预警原因，便于后台快速筛选出需要人工介入的高危病例。
     *
     * @param symptomSummary 触发预警的症状摘要
     * @param reason         触发紧急预警的具体原因或判定依据
     */
    public void recordEmergencyAlert(String symptomSummary, String reason) {
        AiRecallAuditLog entity = AiRecallAuditLog.builder()
                .symptoms(trimText(symptomSummary))
                .status("EMERGENCY_ALERT")
                .message(trimText(reason))
                .build();
        saveQuietly(entity);
    }

    /**
     * 静默保存审计日志实体。
     * <p>
     * 采用 Fail-Safe（失效安全）设计模式：通过 try-catch 包裹数据库保存操作。
     * 即使数据库写入失败或发生异常，也仅打印警告日志，绝不抛出异常中断主业务流程（AI 问诊）。
     *
     * @param entity 待保存的审计日志实体
     */
    private void saveQuietly(AiRecallAuditLog entity) {
        try {
            aiRecallAuditLogRepository.save(entity);
        } catch (RuntimeException ex) {
            // 审计失败不影响主流程。
            log.warn("写入 AI 审计日志失败: {}", ex.getMessage());
        }
    }

    /**
     * 文本截断保护工具方法。
     * <p>
     * 为防止超长文本撑爆数据库字段，限制最大长度为 500 个字符。超出部分将被直接截断。
     *
     * @param input 原始输入字符串
     * @return 经过 trim 处理且长度不超过 MAX_MESSAGE_LENGTH 的字符串；若输入为空则返回 null
     */
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

    /**
     * 将字符串列表安全地转换为 JSON 数组字符串。
     * <p>
     * 主要用于将提取出的疾病码列表持久化到数据库。如果转换过程出现异常，会默认返回空数组字符串 "[]"，
     * 保证数据库中该字段格式的绝对统一。
     *
     * @param values 待转换的字符串列表
     * @return JSON 格式的字符串（例如 "[\"DIS_001\",\"DIS_002\"]"）
     */
    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    /**
     * 从文本中提取标准的疾病编码。
     * <p>
     * 使用正则表达式 {@link #DISEASE_CODE_PATTERN} 匹配文本中的疾病码（如 DIS_1001），
     * 并在内部进行去重处理，确保返回的列表中不包含重复项。
     *
     * @param text 包含疾病码的原始文本（如 AI 回复内容或规则引擎日志）
     * @return 提取并去重后的疾病编码列表
     */
    private List<String> extractDiseaseCodes(String text) {
        List<String> codes = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return codes;
        }
        Matcher matcher = DISEASE_CODE_PATTERN.matcher(text);
        while (matcher.find()) {
            String code = matcher.group(1);
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

    /**
     * 安全解析患者的年龄。
     *
     * @param patient 患者实体
     * @return 患者年龄，若患者实体为空则返回 null
     */
    private Integer resolveAge(Patient patient) {
        return patient == null ? null : patient.getPatientAge();
    }

    /**
     * 安全解析患者的性别编码。
     * <p>
     * 若患者信息缺失或性别未定义，则默认返回 Gender.UNKNOWN 的标准编码，保证数据规范性。
     *
     * @param patient 患者实体
     * @return 患者的性别编码（如 "MALE", "FEMALE", "UNKNOWN"）
     */
    private String resolveGender(Patient patient) {
        if (patient == null || patient.getPatientGender() == null) {
            return Gender.UNKNOWN.getCode();
        }
        return patient.getPatientGender().getCode();
    }

    /**
     * 根据患者年龄计算所属年龄段分组。
     * <p>
     * 将年龄划分为三个标准组别：child (<18岁), adult (18-59岁), elder (>=60岁)。
     * 这有助于后续针对不同人群进行健康数据分析。
     *
     * @param patient 患者实体
     * @return 年龄段分组标识（"child", "adult", "elder" 或 "unknown"）
     */
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

    /**
     * 在消息末尾追加会话 ID 提示。
     * <p>
     * 为了方便日志追踪和问题定位，将当前的 SessionId 拼接到消息内容的末尾。
     *
     * @param message   原始消息内容
     * @param sessionId 会话唯一标识
     * @return 追加了 sessionId 后缀的消息字符串
     */
    private String appendSessionHint(String message, String sessionId) {
        String text = trimText(message);
        if (!StringUtils.hasText(sessionId)) {
            return text;
        }
        String base = StringUtils.hasText(text) ? text : "AI_ANALYZE";
        return trimText(base + " | sessionId=" + sessionId.trim());
    }
}