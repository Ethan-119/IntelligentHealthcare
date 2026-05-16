package com.intelligenthealthcare.triage.application.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.audit.application.AuditApplicationService;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalRepository;
import com.intelligenthealthcare.mcp.ImageVisionTools;
import com.intelligenthealthcare.mcp.MedicalDecisionTools;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import com.intelligenthealthcare.triage.application.AiAnalysisService;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.triage.application.dto.AiSessionConversation;
import com.intelligenthealthcare.triage.application.dto.AiSessionSummary;
import com.intelligenthealthcare.triage.application.dto.AiSessionTurn;
import com.intelligenthealthcare.triage.domain.model.TriageSession;
import com.intelligenthealthcare.triage.domain.model.TriageSlotState;
import com.intelligenthealthcare.triage.domain.model.TriageTurn;
import com.intelligenthealthcare.triage.domain.repository.TriageSessionRepository;
import com.intelligenthealthcare.triage.domain.repository.TriageSlotStateRepository;
import com.intelligenthealthcare.triage.domain.repository.TriageTurnRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

@Service
/**
 * AI 导诊编排服务：
 * 1) 管理会话与轮次持久化；
 * 2) 接入 MCP 图片分析结果；
 * 3) 通过 ReAct 循环驱动工具调用（知识检索 / 医院推荐 / 急症预警 / 会话记忆 / 用户档案）并收敛最终答复。
 */
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private static final int MAX_HISTORY_TURNS = 6;
    private static final long SESSION_CACHE_TTL_MINUTES = 20L;
    private static final String SESSION_HISTORY_CACHE_PREFIX = "ai:session:history:";
    private static final int MAX_OBSERVATION_LENGTH = 1200;
    private static final String NON_MEDICAL_REPLY =
            "我当前仅支持医疗健康相关咨询，例如症状分析、就医建议、检查指标解读等。请描述您的健康问题，我会继续帮助您。";
    private static final List<String> MEDICAL_KEYWORDS = List.of(
            "症状", "发烧", "发热", "咳嗽", "头痛", "头晕", "腹痛", "腹泻", "呕吐", "恶心",
            "胸闷", "胸痛", "心慌", "呼吸", "咽喉", "皮疹", "红肿", "疼痛", "出血", "感染",
            "炎症", "血压", "血糖", "体温", "就医", "医院", "科室", "医生", "挂号", "急诊",
            "药", "用药", "检查", "化验", "检验", "报告", "医疗", "健康", "护理", "康复",
            "诊断", "治疗", "疾病", "病情");

    private final ChatClient chatClient;
    private final PatientRepository patientRepository;
    private final AuditApplicationService auditApplicationService;
    private final HospitalRepository hospitalRepository;
    private final TriageSessionRepository triageSessionRepository;
    private final TriageTurnRepository triageTurnRepository;
    private final TriageSlotStateRepository triageSlotStateRepository;
    private final ObjectMapper objectMapper;
    private final ImageVisionTools imageVisionTools;
    private final MedicalDecisionTools medicalDecisionTools;
    private final RedissonClient redissonClient;
    private final int reactMaxSteps;
    private final int reactRagTopK;
    private final boolean reactDebugTrace;
    private final int maxVisionImages;

    // Lombok @RequiredArgsConstructor 不适用：需要在构造时调用 chatClientBuilder.build()
    public AiAnalysisServiceImpl(
            ChatClient.Builder chatClientBuilder,
            PatientRepository patientRepository,
            AuditApplicationService auditApplicationService,
            HospitalRepository hospitalRepository,
            TriageSessionRepository triageSessionRepository,
            TriageTurnRepository triageTurnRepository,
            TriageSlotStateRepository triageSlotStateRepository,
            ObjectMapper objectMapper,
            ImageVisionTools imageVisionTools,
            MedicalDecisionTools medicalDecisionTools,
            RedissonClient redissonClient,
            @Value("${app.agent.react.max-steps:4}") int reactMaxSteps,
            @Value("${app.agent.react.rag-top-k:3}") int reactRagTopK,
            @Value("${app.agent.react.debug-trace:false}") boolean reactDebugTrace,
            @Value("${app.vision.max-images:5}") int maxVisionImages) {
        this.chatClient = chatClientBuilder.build();
        this.patientRepository = patientRepository;
        this.auditApplicationService = auditApplicationService;
        this.hospitalRepository = hospitalRepository;
        this.triageSessionRepository = triageSessionRepository;
        this.triageTurnRepository = triageTurnRepository;
        this.triageSlotStateRepository = triageSlotStateRepository;
        this.objectMapper = objectMapper;
        this.imageVisionTools = imageVisionTools;
        this.medicalDecisionTools = medicalDecisionTools;
        this.redissonClient = redissonClient;
        this.reactMaxSteps = reactMaxSteps;
        this.reactRagTopK = reactRagTopK;
        this.reactDebugTrace = reactDebugTrace;
        this.maxVisionImages = maxVisionImages;
    }

    // 不使用 @Transactional：方法内包含外部 AI 调用（ChatClient / MCP 视觉分析），
    // 可能耗时数十秒；每个 DB 写操作（saveTurn / updateSessionAfterTurn / audit log）均为独立自动提交，
    // 异常时已写入的错误记录不会被回滚，确保故障可追溯。
    @Override
    public AiAnalyzeResult analyze(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images,
            Double latitude,
            Double longitude) {
        if (!StringUtils.hasText(content)) {
            return new AiAnalyzeResult(resolveSessionId(sessionId), "输入内容为空，无法分析。");
        }
        if (!isMedicalQuery(content)) {
            return new AiAnalyzeResult(resolveSessionId(sessionId), NON_MEDICAL_REPLY, null);
        }

        String resolvedSessionId = resolveSessionId(sessionId);
        String userId = principal.getId().toString();
        TriageSession triageSession = getOrCreateSession(userId, resolvedSessionId);
        applyUserLocation(triageSession, latitude, longitude);
        Patient currentPatient = patientRepository.findById(principal.getId()).orElse(null);
        int turnNo = nextTurnNo(triageSession.getAskRound());
        List<TriageTurn> historyTurns = listRecentTurns(resolvedSessionId);
        List<String> safeImages = sanitizeImages(images);
        boolean hasImages = !safeImages.isEmpty();

        // MCP 集成：通过 MCP ImageVisionTools 调用 DashScope 多模态模型分析图片
        String imageAnalysis = null;
        if (hasImages) {
            imageAnalysis = imageVisionTools.analyzeMedicalImages(
                    safeImages, buildPatientProfileContext(principal) + "\n症状描述：" + content);
        }

        String normalizedContent = normalizeKey(content);
        try {
            // ReAct 主循环：模型按 step 决策 action，直到 FINISH 或达到最大步数。
            ReActResult reactResult = runReActAnalysis(principal, resolvedSessionId, content,
                    hasImages, imageAnalysis, historyTurns);
            String result = reactResult.finalAnswer();
            String rawDecisionJson = buildTurnDecisionJson(
                    content, normalizedContent, safeImages, imageAnalysis, reactResult.trace(), "SUCCESS", null);
            auditApplicationService.recordAiAnalyzeSuccess(
                    content, safeImages, result, currentPatient, resolvedSessionId);
            saveTurn(resolvedSessionId, turnNo, content, normalizedContent, result, rawDecisionJson);
            updateSessionAfterTurn(triageSession, turnNo, 0, principal, result);
            upsertSlotState(resolvedSessionId, historyTurns, content);
            return new AiAnalyzeResult(resolvedSessionId, result, imageAnalysis);
        } catch (RuntimeException ex) {
            String errorText = ex.getMessage() == null ? "未知错误" : ex.getMessage();
            String failedReply = "分析失败：" + errorText;
            String rawDecisionJson = buildTurnDecisionJson(
                    content, normalizedContent, safeImages, imageAnalysis, "", "FAILED", errorText);
            auditApplicationService.recordAiAnalyzeFailed(
                    content, safeImages, ex.getMessage(), currentPatient, resolvedSessionId);
            saveTurn(
                    resolvedSessionId,
                    turnNo,
                    content,
                    normalizedContent,
                    failedReply,
                    rawDecisionJson);
            updateSessionAfterTurn(triageSession, turnNo, 1, principal, failedReply);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSessionSummary> listMySessions(PatientAuthPrincipal principal) {
        List<TriageSession> sessions = triageSessionRepository.findByUserId(principal.getId().toString(), 50);
        List<AiSessionSummary> result = new ArrayList<>(sessions.size());
        for (int i = 0; i < sessions.size(); i++) {
            TriageSession session = sessions.get(i);
            String title = buildSessionTitle(session.getSessionId());
            result.add(new AiSessionSummary(
                    session.getSessionId(),
                    title,
                    session.getAskRound(),
                    session.getUpdateTime()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AiSessionConversation getSessionConversation(PatientAuthPrincipal principal, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId 不能为空");
        }
        TriageSession session = triageSessionRepository.findBySessionId(sessionId.trim()).orElse(null);
        if (session == null || !principal.getId().toString().equals(session.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        List<TriageTurn> turns = triageTurnRepository.findAllBySessionId(sessionId.trim());
        List<AiSessionTurn> items = new ArrayList<>(turns.size());
        for (int i = 0; i < turns.size(); i++) {
            TriageTurn turn = turns.get(i);
            items.add(new AiSessionTurn(
                    turn.getTurnNo(),
                    turn.getUserMessage(),
                    turn.getReplyText(),
                    turn.getCreateTime()));
        }
        items.sort(Comparator.comparing(AiSessionTurn::turnNo));
        return new AiSessionConversation(sessionId.trim(), items);
    }

    private String buildPrompt(String content, boolean hasImages, String imageAnalysis, List<TriageTurn> historyTurns) {
        StringBuilder sb = new StringBuilder();
        if (!historyTurns.isEmpty()) {
            sb.append("以下是本次会话的历史上下文，请结合上下文保持连续性：\n");
            for (int i = 0; i < historyTurns.size(); i++) {
                TriageTurn turn = historyTurns.get(i);
                sb.append("用户：").append(defaultText(turn.getUserMessage())).append("\n");
                sb.append("助手：").append(defaultText(turn.getReplyText())).append("\n");
            }
            sb.append("\n");
        }
        sb.append("""
                你是医疗辅助分析助手。请基于以下内容给出结构化分析：
                1. 关键信息提取
                2. 初步风险提示
                3. 建议下一步检查方向
                4. 说明这不是最终临床诊断
                """);
        if (hasImages && imageAnalysis != null && !imageAnalysis.isBlank()) {
            sb.append("5. 以下为 MCP 视觉服务对用户上传图片的分析结果，请结合此结果给出综合建议：\n");
            sb.append("```\n").append(imageAnalysis).append("\n```\n");
        } else if (hasImages) {
            sb.append("5. 用户已上传图片（图片分析待完成），请结合文本描述给出初步分析\n");
        }
        sb.append("\n待分析内容：\n").append(content);
        return sb.toString();
    }

    private ReActResult runReActAnalysis(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            String imageAnalysis,
            List<TriageTurn> historyTurns) {
        StringBuilder scratchpad = new StringBuilder();
        for (int step = 1; step <= reactMaxSteps; step++) {
            ReActDecision decision = planNextAction(
                    principal, sessionId, content, hasImages, imageAnalysis, historyTurns, scratchpad, step);
            String action = normalizeAction(decision.action());
            if ("FINISH".equals(action)) {
                String finalAnswer = finalizeAnswer(
                        content, hasImages, imageAnalysis, historyTurns, scratchpad, decision.finalAnswer());
                return new ReActResult(finalAnswer, scratchpad.toString());
            }
            // 非 FINISH 动作先执行并记录 Observation，再进入下一轮决策。
            String observation = executeAction(
                    action,
                    decision.actionInput(),
                    principal,
                    sessionId,
                    content,
                    historyTurns,
                    triageSessionRepository.findBySessionId(sessionId).orElse(null));
            appendScratchpad(scratchpad, step, decision, observation);
        }
        // 到达最大步数仍未 FINISH 时，强制收敛生成最终回答，避免无限推理。
        String finalAnswer = finalizeAnswer(content, hasImages, imageAnalysis, historyTurns, scratchpad, "");
        return new ReActResult(finalAnswer, scratchpad.toString());
    }

    private ReActDecision planNextAction(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            String imageAnalysis,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            int stepNo) {
        String plannerPrompt = buildReActPlannerPrompt(principal, sessionId, content,
                hasImages, imageAnalysis, historyTurns, scratchpad, stepNo);
        String raw = chatClient.prompt(plannerPrompt).call().content();
        return parseDecision(raw, content);
    }

    private String buildReActPlannerPrompt(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            String imageAnalysis,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            int stepNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是医疗导诊 Agent，必须使用 ReAct 方式决策（先思考，再行动，最后给结论）。
                你每一步只能返回一个 JSON（不要 markdown 代码块，不要额外文本）：
                {
                  "thought": "本步简短思考",
                  "action": "SEARCH_MEDICAL_KNOWLEDGE|RECOMMEND_HOSPITAL|LOG_EMERGENCY|SESSION_MEMORY|PATIENT_PROFILE|FINISH",
                  "actionInput": "动作输入，可为空字符串",
                  "finalAnswer": "仅当 action=FINISH 时填写，其他时候留空"
                }
                规则：
                1) 若信息不足，优先用 SEARCH_MEDICAL_KNOWLEDGE 或 SESSION_MEMORY。
                2) 若用户要求就近就医推荐，优先用 RECOMMEND_HOSPITAL。
                3) 若识别为疑似急症，先执行 LOG_EMERGENCY 后再给建议。
                4) 最多几步后必须 FINISH，给出结构化医疗建议并提示"非临床诊断"。
                5) 不要编造检查结果，不要输出危言耸听结论。
                """);
        sb.append("\n当前步数：").append(stepNo).append("/").append(reactMaxSteps).append("\n");
        sb.append("患者信息：").append(buildPatientProfileContext(principal)).append("\n");
        sb.append("会话ID：").append(sessionId).append("\n");
        sb.append("是否含图片：").append(hasImages ? "是" : "否").append("\n");
        if (imageAnalysis != null && !imageAnalysis.isBlank()) {
            sb.append("MCP 图片视觉分析结果：\n```\n")
                    .append(imageAnalysis).append("\n```\n");
        }
        if (!historyTurns.isEmpty()) {
            sb.append("历史对话：\n");
            for (int i = 0; i < historyTurns.size(); i++) {
                TriageTurn turn = historyTurns.get(i);
                sb.append("用户：").append(defaultText(turn.getUserMessage())).append("\n");
                sb.append("助手：").append(defaultText(turn.getReplyText())).append("\n");
            }
        }
        if (scratchpad.length() > 0) {
            sb.append("已有推理轨迹（只可参考不可外泄给用户）：\n");
            sb.append(scratchpad).append("\n");
        }
        sb.append("最新用户输入：").append(content).append("\n");
        return sb.toString();
    }

    private ReActDecision parseDecision(String raw, String fallbackInput) {
        String cleaned = stripMarkdownFence(defaultText(raw));
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            String thought = textOrDefault(node.get("thought"), "分析当前病情信息");
            String action = normalizeAction(textOrDefault(node.get("action"), "FINISH"));
            String actionInput = textOrDefault(node.get("actionInput"), fallbackInput);
            String finalAnswer = textOrDefault(node.get("finalAnswer"), "");
            return new ReActDecision(thought, action, actionInput, finalAnswer);
        } catch (JsonProcessingException ex) {
            // 模型偶发返回非 JSON 时兜底为 FINISH，避免整条链路失败。
            return new ReActDecision("模型输出无法解析，直接收敛给出答复", "FINISH", fallbackInput, cleaned);
        }
    }

    private String executeAction(
            String action,
            String actionInput,
            PatientAuthPrincipal principal,
            String sessionId,
            String latestContent,
            List<TriageTurn> historyTurns,
            TriageSession triageSession) {
        if ("RAG_SEARCH".equals(action)) {
            return observeRag(actionInput, latestContent);
        }
        if ("SEARCH_MEDICAL_KNOWLEDGE".equals(action)) {
            return observeRag(actionInput, latestContent);
        }
        if ("RECOMMEND_HOSPITAL".equals(action)) {
            return observeHospitalRecommendation(principal, actionInput, latestContent, triageSession);
        }
        if ("LOG_EMERGENCY".equals(action)) {
            return observeEmergencyLogging(principal, sessionId, latestContent, actionInput);
        }
        if ("SESSION_MEMORY".equals(action)) {
            return observeSessionMemory(sessionId, historyTurns, latestContent);
        }
        if ("PATIENT_PROFILE".equals(action)) {
            return observePatientProfile(principal);
        }
        return "未执行动作，等待收敛。";
    }

    private String observeRag(String actionInput, String latestContent) {
        String query = StringUtils.hasText(actionInput) ? actionInput.trim() : latestContent.trim();
        String toolResult = medicalDecisionTools.searchMedicalKnowledge(query, reactRagTopK);
        return truncateObservation(toolResult);
    }

    private String observeHospitalRecommendation(
            PatientAuthPrincipal principal,
            String actionInput,
            String latestContent,
            TriageSession triageSession) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        String city = patient == null ? "" : patient.getResidentCity();
        String area = patient == null ? "" : patient.getArea();
        String symptomSummary = StringUtils.hasText(actionInput) ? actionInput.trim() : latestContent.trim();
        // 从会话中读取前端通过浏览器 Geolocation API 传入的用户坐标，用于 Haversine 距离排序。
        BigDecimal latitude = triageSession == null ? null : triageSession.getLatitude();
        BigDecimal longitude = triageSession == null ? null : triageSession.getLongitude();
        String toolResult = medicalDecisionTools.recommendHospital(city, area, symptomSummary, latitude, longitude);
        return truncateObservation(toolResult);
    }

    private String observeEmergencyLogging(
            PatientAuthPrincipal principal,
            String sessionId,
            String latestContent,
            String actionInput) {
        String reason = StringUtils.hasText(actionInput) ? actionInput.trim() : "模型判定疑似急症";
        String toolResult = medicalDecisionTools.logEmergency(
                latestContent,
                reason,
                sessionId,
                principal.getId().toString());
        return truncateObservation(toolResult);
    }

    private String observeSessionMemory(String sessionId, List<TriageTurn> historyTurns, String latestContent) {
        TriageSlotState slotState = triageSlotStateRepository.findBySessionId(sessionId).orElse(null);
        StringBuilder sb = new StringBuilder();
        sb.append("会话近历史轮数=").append(historyTurns.size());
        sb.append("；最新输入=").append(latestContent.trim());
        if (slotState != null && StringUtils.hasText(slotState.getSymptomsJson())) {
            sb.append("；已记录症状槽位=").append(slotState.getSymptomsJson());
        } else {
            sb.append("；症状槽位为空");
        }
        return truncateObservation(sb.toString());
    }

    private String observePatientProfile(PatientAuthPrincipal principal) {
        return "患者基础档案：" + buildPatientProfileContext(principal);
    }

    private String buildPatientProfileContext(PatientAuthPrincipal principal) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        if (patient == null) {
            return "id=" + principal.getId()
                    + ", username=" + defaultText(principal.getUsername())
                    + ", role=" + (principal.getRole() == null ? "" : principal.getRole().getCode())
                    + ", city=未找到用户, area=未找到用户";
        }
        String gender = patient.getPatientGender() == null ? "未填写" : patient.getPatientGender().getCode();
        String prefer = patient.getTriagePrefer() == null ? "未填写" : patient.getTriagePrefer().getCode();
        return "id=" + patient.getId()
                + ", username=" + valueOrUnknown(patient.getUsername())
                + ", role=" + (patient.getRole() == null ? "" : patient.getRole().getCode())
                + ", age=" + (patient.getPatientAge() == null ? "未填写" : patient.getPatientAge())
                + ", gender=" + gender
                + ", city=" + valueOrUnknown(patient.getResidentCity())
                + ", area=" + valueOrUnknown(patient.getArea())
                + ", triagePrefer=" + prefer;
    }

    private String valueOrUnknown(String value) {
        if (!StringUtils.hasText(value)) {
            return "未填写";
        }
        return value.trim();
    }

    private String buildSessionTitle(String sessionId) {
        List<TriageTurn> turns = triageTurnRepository.findAllBySessionId(sessionId);
        if (turns.isEmpty()) {
            return "新会话";
        }
        for (int i = 0; i < turns.size(); i++) {
            String userText = turns.get(i).getUserMessage();
            if (StringUtils.hasText(userText)) {
                String trimmed = userText.trim();
                if (trimmed.length() <= 24) {
                    return trimmed;
                }
                return trimmed.substring(0, 24) + "...";
            }
        }
        return "新会话";
    }

    private String finalizeAnswer(
            String content,
            boolean hasImages,
            String imageAnalysis,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            String draftAnswer) {
        if (StringUtils.hasText(draftAnswer)) {
            if (reactDebugTrace) {
                return draftAnswer.trim() + "\n\n---\nReAct Trace(调试)：\n" + scratchpad;
            }
            return draftAnswer.trim();
        }
        String finalPrompt = buildPrompt(content, hasImages, imageAnalysis, historyTurns)
                + "\n\n以下是 ReAct 过程中的关键信息，请据此给出最终答复：\n"
                + scratchpad
                + "\n\n输出要求：\n"
                + "1. 关键信息提取\n"
                + "2. 初步风险提示\n"
                + "3. 建议下一步检查方向\n"
                + "4. 明确声明\u201c这不是最终临床诊断\u201d。\n";
        String answer = chatClient.prompt(finalPrompt).call().content();
        if (!reactDebugTrace) {
            return answer;
        }
        return answer + "\n\n---\nReAct Trace(调试)：\n" + scratchpad;
    }

    private void appendScratchpad(StringBuilder scratchpad, int step, ReActDecision decision, String observation) {
        scratchpad.append("Step ").append(step).append("\n");
        scratchpad.append("Thought: ").append(defaultText(decision.thought())).append("\n");
        scratchpad.append("Action: ").append(defaultText(decision.action())).append("\n");
        scratchpad.append("ActionInput: ").append(defaultText(decision.actionInput())).append("\n");
        scratchpad.append("Observation: ").append(defaultText(observation)).append("\n\n");
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return "FINISH";
        }
        String normalized = action.trim().toUpperCase();
        if ("SEARCH_MEDICAL_KNOWLEDGE".equals(normalized)
                || "RECOMMEND_HOSPITAL".equals(normalized)
                || "LOG_EMERGENCY".equals(normalized)
                || "SESSION_MEMORY".equals(normalized)
                || "PATIENT_PROFILE".equals(normalized)
                || "FINISH".equals(normalized)) {
            return normalized;
        }
        if ("RAG_SEARCH".equals(normalized)
                || "SEARCH_KNOWLEDGE".equals(normalized)) {
            return "SEARCH_MEDICAL_KNOWLEDGE";
        }
        return "FINISH";
    }

    private String truncateObservation(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_OBSERVATION_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_OBSERVATION_LENGTH);
    }

    private String stripMarkdownFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                return trimmed.substring(firstLine + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String textOrDefault(JsonNode node, String fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        String value = node.asText();
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private boolean isMedicalQuery(String content) {
        if (isClearlyMedicalByKeywords(content)) {
            return true;
        }
        return isMedicalByModel(content);
    }

    private boolean isClearlyMedicalByKeywords(String content) {
        String normalized = content == null ? "" : content.toLowerCase(Locale.ROOT);
        for (int i = 0; i < MEDICAL_KEYWORDS.size(); i++) {
            if (normalized.contains(MEDICAL_KEYWORDS.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMedicalByModel(String content) {
        String classifyPrompt = """
                请判断用户输入是否属于医疗健康咨询。
                只允许输出 YES 或 NO，不要输出其它内容。
                YES：与症状、疾病、就医、检查、药物、护理、健康风险相关。
                NO：与医疗健康无关（如编程、娱乐、政治、购物、旅行等）。
                用户输入：
                """ + content;
        try {
            String result = chatClient.prompt(classifyPrompt).call().content();
            if (!StringUtils.hasText(result)) {
                return true;
            }
            String normalized = result.trim().toUpperCase(Locale.ROOT);
            return normalized.startsWith("YES");
        } catch (RuntimeException ex) {
            // 分类失败时保守放行，避免误伤真实医疗问题。
            return true;
        }
    }

    private TriageSession getOrCreateSession(String userId, String sessionId) {
        TriageSession existing = triageSessionRepository.findByUserIdAndSessionId(userId, sessionId).orElse(null);
        if (existing != null) {
            return existing;
        }
        TriageSession created = TriageSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .dialogId(sessionId)
                .status("active")
                .currentStage("analysis")
                .askRound(0)
                .invalidAnswerCount(0)
                .build();
        triageSessionRepository.save(created);
        return created;
    }

    private List<TriageTurn> listRecentTurns(String sessionId) {
        List<TriageTurn> cached = readRecentTurnsFromCache(sessionId);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<TriageTurn> loaded = triageTurnRepository.findRecentBySessionId(sessionId, MAX_HISTORY_TURNS);
        writeRecentTurnsToCache(sessionId, loaded);
        return loaded;
    }

    private void saveTurn(
            String sessionId,
            int turnNo,
            String content,
            String normalizedContent,
            String reply,
            String rawDecisionJson) {
        TriageTurn turn = TriageTurn.builder()
                .sessionId(sessionId)
                .turnNo(turnNo)
                .userMessage(content)
                .normalizedQuery(normalizedContent)
                .intent("AI_ANALYZE")
                .stage("analysis")
                .replyText(reply)
                .rawDecisionJson(rawDecisionJson)
                .build();
        triageTurnRepository.save(turn);
        appendTurnToCache(sessionId, turn);
    }

    private void updateSessionAfterTurn(
            TriageSession triageSession,
            int askRound,
            int invalidIncrement,
            PatientAuthPrincipal principal,
            String replyText) {
        Integer invalidCount = triageSession.getInvalidAnswerCount();
        if (invalidCount == null) {
            invalidCount = 0;
        }
        triageSession.setAskRound(askRound);
        triageSession.setInvalidAnswerCount(invalidCount + invalidIncrement);
        triageSession.setCurrentStage("analysis");
        triageSession.setStatus("active");
        fillSessionProfileFromPatient(triageSession, principal);
        fillSessionDecisionSnapshot(triageSession, replyText);
        triageSessionRepository.updateById(triageSession);
    }

    private void fillSessionProfileFromPatient(TriageSession triageSession, PatientAuthPrincipal principal) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        if (patient == null) {
            return;
        }
        // 会话快照同步用户档案，便于后续“就近推荐”直接读取 triage_session。
        triageSession.setCity(trimToNull(patient.getResidentCity()));
        triageSession.setArea(trimToNull(patient.getArea()));
        triageSession.setPatientAge(patient.getPatientAge());
        triageSession.setPatientGender(patient.getPatientGender() == null ? null : patient.getPatientGender().getCode());
        TriagePrefer prefer = patient.getTriagePrefer();
        if (prefer != null) {
            triageSession.setNearby(TriagePrefer.NEARBY.equals(prefer) ? 1 : 0);
        }
    }

    private void fillSessionDecisionSnapshot(TriageSession triageSession, String replyText) {
        if (!StringUtils.hasText(replyText)) {
            return;
        }
        String text = replyText.trim();
        triageSession.setSeverityLevel(inferSeverityLevel(text));
        triageSession.setRouteType(inferRouteType(text));

        Hospital matched = findHospitalMentioned(text);
        if (matched == null) {
            return;
        }
        if (!StringUtils.hasText(triageSession.getCity()) && StringUtils.hasText(matched.getCity())) {
            triageSession.setCity(matched.getCity().trim());
        }
        if (!StringUtils.hasText(triageSession.getArea()) && StringUtils.hasText(matched.getDistrictName())) {
            triageSession.setArea(matched.getDistrictName().trim());
        }
        if (!StringUtils.hasText(triageSession.getRouteType())
                || "DISEASE".equals(triageSession.getRouteType())
                || "ANALYSIS".equals(triageSession.getRouteType())) {
            triageSession.setRouteType("HOSPITAL");
        }
    }

    private String inferSeverityLevel(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "急症", "紧急", "立即", "立刻", "120", "急救", "危险")) {
            return "high";
        }
        if (containsAny(normalized, "尽快", "重视", "风险", "建议就医", "尽快就医")) {
            return "medium";
        }
        return "low";
    }

    private String inferRouteType(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "急诊", "急救", "120", "紧急", "立即")) {
            return "EMERGENCY";
        }
        if (containsAny(normalized, "医院", "就医推荐", "就近就医")) {
            return "HOSPITAL";
        }
        if (containsAny(normalized, "科室", "门诊", "挂号")) {
            return "DEPARTMENT";
        }
        if (containsAny(normalized, "疾病", "诊断", "可能", "病")) {
            return "DISEASE";
        }
        return "ANALYSIS";
    }

    private Hospital findHospitalMentioned(String text) {
        if (!StringUtils.hasText(text) || !text.contains("医院")) {
            return null;
        }
        List<Hospital> hospitals = hospitalRepository.findAllActive();
        Hospital best = null;
        int bestNameLength = -1;
        for (int i = 0; i < hospitals.size(); i++) {
            Hospital hospital = hospitals.get(i);
            if (hospital == null || !StringUtils.hasText(hospital.getHospitalName())) {
                continue;
            }
            String name = hospital.getHospitalName().trim();
            if (!text.contains(name)) {
                continue;
            }
            if (name.length() > bestNameLength) {
                best = hospital;
                bestNameLength = name.length();
            }
        }
        return best;
    }

    private boolean containsAny(String text, String... keywords) {
        for (int i = 0; i < keywords.length; i++) {
            if (text.contains(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    private void upsertSlotState(String sessionId, List<TriageTurn> historyTurns, String latestContent) {
        List<String> symptoms = new ArrayList<>();
        for (int i = 0; i < historyTurns.size(); i++) {
            TriageTurn turn = historyTurns.get(i);
            if (StringUtils.hasText(turn.getUserMessage())) {
                addUnique(symptoms, turn.getUserMessage().trim());
            }
        }
        addUnique(symptoms, latestContent.trim());
        String symptomsJson = toJson(symptoms);
        String missingSlotsJson = toJson(buildMissingSlots(null, null, null, null));

        TriageSlotState existing = triageSlotStateRepository.findBySessionId(sessionId).orElse(null);
        if (existing == null) {
            TriageSlotState created = TriageSlotState.builder()
                    .sessionId(sessionId)
                    .symptomsJson(symptomsJson)
                    .missingSlotsJson(missingSlotsJson)
                    .build();
            triageSlotStateRepository.save(created);
            return;
        }
        existing.setSymptomsJson(symptomsJson);
        existing.setMissingSlotsJson(missingSlotsJson);
        triageSlotStateRepository.updateById(existing);
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private String buildTurnDecisionJson(
            String content,
            String normalizedContent,
            List<String> images,
            String imageAnalysis,
            String reactTrace,
            String status,
            String errorMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stage", "analysis");
        payload.put("status", status);
        payload.put("userMessage", defaultText(content));
        payload.put("normalizedQuery", defaultText(normalizedContent));
        payload.put("imageCount", images == null ? 0 : images.size());
        payload.put("images", summarizeImages(images));
        payload.put("imageAnalysis", defaultText(imageAnalysis));
        payload.put("reactTrace", defaultText(reactTrace));
        payload.put("errorMessage", defaultText(errorMessage));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"stage\":\"analysis\",\"status\":\"SERIALIZE_FAILED\"}";
        }
    }

    private List<String> summarizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String img = images.get(i);
            if (!StringUtils.hasText(img)) {
                continue;
            }
            String compact = img.trim();
            int len = compact.length();
            String head = compact.substring(0, Math.min(80, len));
            result.add(head + "...(len=" + len + ")");
        }
        return result;
    }

    private List<String> buildMissingSlots(String diseaseName, String targetHospital, String targetDepartment, String targetDoctor) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(diseaseName)) {
            missing.add("disease_name");
        }
        if (!StringUtils.hasText(targetHospital)) {
            missing.add("target_hospital");
        }
        if (!StringUtils.hasText(targetDepartment)) {
            missing.add("target_department");
        }
        if (!StringUtils.hasText(targetDoctor)) {
            missing.add("target_doctor");
        }
        return missing;
    }

    private void addUnique(List<String> values, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalized = value.trim();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(normalized)) {
                return;
            }
        }
        values.add(normalized);
    }

    private String defaultText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    // 将前端通过浏览器 W3C Geolocation API 获取的用户坐标写入会话。
    // 移动端源自 GPS 芯片，桌面端源自 Wi-Fi 三角定位 / IP 粗略定位。
    // 坐标可为 null（用户拒绝授权、浏览器不支持、或定位超时时前端不传）。
    private void applyUserLocation(TriageSession triageSession, Double latitude, Double longitude) {
        BigDecimal lat = normalizeCoordinate(latitude);
        BigDecimal lon = normalizeCoordinate(longitude);
        if (lat != null) {
            triageSession.setLatitude(lat);
        }
        if (lon != null) {
            triageSession.setLongitude(lon);
        }
    }

    private BigDecimal normalizeCoordinate(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private List<TriageTurn> readRecentTurnsFromCache(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Collections.emptyList();
        }
        try {
            RBucket<String> bucket = redissonClient.getBucket(historyCacheKey(sessionId));
            String json = bucket.get();
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            List<CachedTurn> cachedTurns = objectMapper.readerForListOf(CachedTurn.class).readValue(json);
            List<TriageTurn> result = new ArrayList<>();
            for (int i = 0; i < cachedTurns.size(); i++) {
                CachedTurn item = cachedTurns.get(i);
                if (item == null) {
                    continue;
                }
                result.add(TriageTurn.builder()
                        .sessionId(sessionId)
                        .turnNo(item.turnNo() == null ? i + 1 : item.turnNo())
                        .userMessage(item.userMessage())
                        .replyText(item.replyText())
                        .build());
            }
            return result;
        } catch (RuntimeException | JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private void writeRecentTurnsToCache(String sessionId, List<TriageTurn> turns) {
        if (!StringUtils.hasText(sessionId) || turns == null) {
            return;
        }
        List<CachedTurn> compact = new ArrayList<>();
        int start = Math.max(turns.size() - MAX_HISTORY_TURNS, 0);
        for (int i = start; i < turns.size(); i++) {
            TriageTurn turn = turns.get(i);
            compact.add(new CachedTurn(turn.getTurnNo(), turn.getUserMessage(), turn.getReplyText()));
        }
        writeCachedTurns(sessionId, compact);
    }

    private void appendTurnToCache(String sessionId, TriageTurn turn) {
        if (!StringUtils.hasText(sessionId) || turn == null) {
            return;
        }
        List<CachedTurn> compact = new ArrayList<>();
        List<TriageTurn> existing = readRecentTurnsFromCache(sessionId);
        for (int i = 0; i < existing.size(); i++) {
            TriageTurn item = existing.get(i);
            compact.add(new CachedTurn(item.getTurnNo(), item.getUserMessage(), item.getReplyText()));
        }
        compact.add(new CachedTurn(turn.getTurnNo(), turn.getUserMessage(), turn.getReplyText()));
        if (compact.size() > MAX_HISTORY_TURNS) {
            compact = new ArrayList<>(compact.subList(compact.size() - MAX_HISTORY_TURNS, compact.size()));
        }
        writeCachedTurns(sessionId, compact);
    }

    private void writeCachedTurns(String sessionId, List<CachedTurn> compact) {
        try {
            String json = objectMapper.writeValueAsString(compact);
            RBucket<String> bucket = redissonClient.getBucket(historyCacheKey(sessionId));
            bucket.set(json, Duration.ofMinutes(SESSION_CACHE_TTL_MINUTES));
        } catch (RuntimeException | JsonProcessingException ex) {
            // 缓存写入失败不影响主流程
        }
    }

    private String historyCacheKey(String sessionId) {
        return SESSION_HISTORY_CACHE_PREFIX + sessionId.trim();
    }

    private List<String> sanitizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        int limit = Math.max(maxVisionImages, 1);
        for (int i = 0; i < images.size(); i++) {
            String raw = images.get(i);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            // 前端上传的是 data URL / base64 文本，这里只做清洗与限流，不做解码。
            result.add(raw.trim());
            // 与 app.vision.max-images 对齐，避免单次请求携带过多图片拖慢视觉分析。
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private String resolveSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private int nextTurnNo(Integer askRound) {
        if (askRound == null || askRound < 0) {
            return 1;
        }
        return askRound + 1;
    }

    String normalizeKey(String content) {
        if (content == null) {
            return "empty";
        }
        String trimmed = content.trim();
        // 前 200 字符作为缓存 key，足以区分不同症状描述
        if (trimmed.length() <= 200) {
            return trimmed;
        }
        return trimmed.substring(0, 200);
    }

    private record ReActDecision(String thought, String action, String actionInput, String finalAnswer) {
    }

    private record ReActResult(String finalAnswer, String trace) {
    }

    private record CachedTurn(Integer turnNo, String userMessage, String replyText) {
    }
}
