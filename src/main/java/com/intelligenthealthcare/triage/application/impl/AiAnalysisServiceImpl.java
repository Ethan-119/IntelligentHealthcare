package com.intelligenthealthcare.triage.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.audit.application.AuditApplicationService;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import com.intelligenthealthcare.rag.application.RagQueryService;
import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.triage.application.AiAnalysisService;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.triage.domain.model.TriageSession;
import com.intelligenthealthcare.triage.domain.model.TriageSlotState;
import com.intelligenthealthcare.triage.domain.model.TriageTurn;
import com.intelligenthealthcare.triage.infrastructure.persistence.TriageSessionMapper;
import com.intelligenthealthcare.triage.infrastructure.persistence.TriageSlotStateMapper;
import com.intelligenthealthcare.triage.infrastructure.persistence.TriageTurnMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_OBSERVATION_LENGTH = 1200;

    private final ChatClient chatClient;
    private final RagQueryService ragQueryService;
    private final PatientRepository patientRepository;
    private final AuditApplicationService auditApplicationService;
    private final TriageSessionMapper triageSessionMapper;
    private final TriageTurnMapper triageTurnMapper;
    private final TriageSlotStateMapper triageSlotStateMapper;
    private final ObjectMapper objectMapper;
    private final int reactMaxSteps;
    private final int reactRagTopK;
    private final boolean reactDebugTrace;

    public AiAnalysisServiceImpl(
            ChatClient.Builder chatClientBuilder,
            RagQueryService ragQueryService,
            PatientRepository patientRepository,
            AuditApplicationService auditApplicationService,
            TriageSessionMapper triageSessionMapper,
            TriageTurnMapper triageTurnMapper,
            TriageSlotStateMapper triageSlotStateMapper,
            ObjectMapper objectMapper,
            @Value("${app.agent.react.max-steps:4}") int reactMaxSteps,
            @Value("${app.agent.react.rag-top-k:3}") int reactRagTopK,
            @Value("${app.agent.react.debug-trace:false}") boolean reactDebugTrace) {
        this.chatClient = chatClientBuilder.build();
        this.ragQueryService = ragQueryService;
        this.patientRepository = patientRepository;
        this.auditApplicationService = auditApplicationService;
        this.triageSessionMapper = triageSessionMapper;
        this.triageTurnMapper = triageTurnMapper;
        this.triageSlotStateMapper = triageSlotStateMapper;
        this.objectMapper = objectMapper;
        this.reactMaxSteps = reactMaxSteps;
        this.reactRagTopK = reactRagTopK;
        this.reactDebugTrace = reactDebugTrace;
    }

    @Override
    @Transactional
    public AiAnalyzeResult analyze(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images) {
        if (!StringUtils.hasText(content)) {
            return new AiAnalyzeResult(resolveSessionId(sessionId), "输入内容为空，无法分析。");
        }

        String resolvedSessionId = resolveSessionId(sessionId);
        String userId = principal.getId().toString();
        TriageSession triageSession = getOrCreateSession(userId, resolvedSessionId);
        int turnNo = nextTurnNo(triageSession.getAskRound());
        List<TriageTurn> historyTurns = listRecentTurns(resolvedSessionId);
        boolean hasImages = images != null && !images.isEmpty();

        // TODO: MCP 集成 — 调用外部视觉服务分析图片，结果合并到最终输出
        if (hasImages) {
            // 后续通过 MCP 调用 DashScope 多模态 / 第三方医学影像服务
            // String imageAnalysis = mcpVisionClient.analyze(images, context);
        }

        String normalizedContent = normalizeKey(content);
        try {
            String result = runReActAnalysis(principal, resolvedSessionId, content, hasImages, historyTurns);
            auditApplicationService.recordAiAnalyzeSuccess(content, images, result);
            saveTurn(resolvedSessionId, turnNo, content, normalizedContent, result);
            updateSessionAfterTurn(triageSession, turnNo, 0, principal);
            upsertSlotState(resolvedSessionId, historyTurns, content);
            return new AiAnalyzeResult(resolvedSessionId, result);
        } catch (RuntimeException ex) {
            auditApplicationService.recordAiAnalyzeFailed(content, images, ex.getMessage());
            saveTurn(
                    resolvedSessionId,
                    turnNo,
                    content,
                    normalizedContent,
                    "分析失败：" + (ex.getMessage() == null ? "未知错误" : ex.getMessage()));
            updateSessionAfterTurn(triageSession, turnNo, 1, principal);
            throw ex;
        }
    }

    private String buildPrompt(String content, boolean hasImages, List<TriageTurn> historyTurns) {
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
        if (hasImages) {
            sb.append("5. 用户已上传图片，图片分析结果将由外部视觉服务补充，请结合文本描述给出初步分析\n");
        }
        sb.append("\n待分析内容：\n").append(content);
        return sb.toString();
    }

    private String runReActAnalysis(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            List<TriageTurn> historyTurns) {
        StringBuilder scratchpad = new StringBuilder();
        for (int step = 1; step <= reactMaxSteps; step++) {
            ReActDecision decision = planNextAction(
                    principal, sessionId, content, hasImages, historyTurns, scratchpad, step);
            String action = normalizeAction(decision.action());
            if ("FINISH".equals(action)) {
                return finalizeAnswer(content, hasImages, historyTurns, scratchpad, decision.finalAnswer());
            }
            String observation = executeAction(action, decision.actionInput(), principal, sessionId, content, historyTurns);
            appendScratchpad(scratchpad, step, decision, observation);
        }
        return finalizeAnswer(content, hasImages, historyTurns, scratchpad, "");
    }

    private ReActDecision planNextAction(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            int stepNo) {
        String plannerPrompt = buildReActPlannerPrompt(principal, sessionId, content, hasImages, historyTurns, scratchpad, stepNo);
        String raw = chatClient.prompt(plannerPrompt).call().content();
        return parseDecision(raw, content);
    }

    private String buildReActPlannerPrompt(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            boolean hasImages,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            int stepNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是医疗导诊 Agent，必须使用 ReAct 方式决策（先思考，再行动，最后给结论）。
                你每一步只能返回一个 JSON（不要 markdown 代码块，不要额外文本）：
                {
                  "thought": "本步简短思考",
                  "action": "RAG_SEARCH|SESSION_MEMORY|PATIENT_PROFILE|FINISH",
                  "actionInput": "动作输入，可为空字符串",
                  "finalAnswer": "仅当 action=FINISH 时填写，其他时候留空"
                }
                规则：
                1) 若信息不足，优先用 RAG_SEARCH 或 SESSION_MEMORY。
                2) 最多几步后必须 FINISH，给出结构化医疗建议并提示“非临床诊断”。
                3) 不要编造检查结果，不要输出危言耸听结论。
                """);
        sb.append("\n当前步数：").append(stepNo).append("/").append(reactMaxSteps).append("\n");
        sb.append("患者信息：").append(buildPatientProfileContext(principal)).append("\n");
        sb.append("会话ID：").append(sessionId).append("\n");
        sb.append("是否含图片：").append(hasImages ? "是" : "否").append("\n");
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
            return new ReActDecision("模型输出无法解析，直接收敛给出答复", "FINISH", fallbackInput, cleaned);
        }
    }

    private String executeAction(
            String action,
            String actionInput,
            PatientAuthPrincipal principal,
            String sessionId,
            String latestContent,
            List<TriageTurn> historyTurns) {
        if ("RAG_SEARCH".equals(action)) {
            return observeRag(actionInput, latestContent);
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
        List<RagSearchHitDto> hits = ragQueryService.search(query, reactRagTopK);
        if (hits.isEmpty()) {
            return "RAG 检索无匹配知识，请基于通用临床安全原则继续。";
        }
        StringBuilder sb = new StringBuilder("RAG 命中：");
        for (int i = 0; i < hits.size(); i++) {
            RagSearchHitDto hit = hits.get(i);
            sb.append("\n- [").append(i + 1).append("] ")
                    .append(defaultText(hit.content()))
                    .append(" (source=").append(defaultText(hit.sourceType()))
                    .append(", score=").append(hit.distance()).append(")");
        }
        return truncateObservation(sb.toString());
    }

    private String observeSessionMemory(String sessionId, List<TriageTurn> historyTurns, String latestContent) {
        LambdaQueryWrapper<TriageSlotState> query = new LambdaQueryWrapper<>();
        query.eq(TriageSlotState::getSessionId, sessionId);
        TriageSlotState slotState = triageSlotStateMapper.selectOne(query);
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

    private String finalizeAnswer(
            String content,
            boolean hasImages,
            List<TriageTurn> historyTurns,
            StringBuilder scratchpad,
            String draftAnswer) {
        if (StringUtils.hasText(draftAnswer)) {
            if (reactDebugTrace) {
                return draftAnswer.trim() + "\n\n---\nReAct Trace(调试)：\n" + scratchpad;
            }
            return draftAnswer.trim();
        }
        String finalPrompt = buildPrompt(content, hasImages, historyTurns)
                + "\n\n以下是 ReAct 过程中的关键信息，请据此给出最终答复：\n"
                + scratchpad
                + "\n\n输出要求：\n"
                + "1. 关键信息提取\n"
                + "2. 初步风险提示\n"
                + "3. 建议下一步检查方向\n"
                + "4. 明确声明“这不是最终临床诊断”。\n";
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
        if ("RAG_SEARCH".equals(normalized)
                || "SESSION_MEMORY".equals(normalized)
                || "PATIENT_PROFILE".equals(normalized)
                || "FINISH".equals(normalized)) {
            return normalized;
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

    private TriageSession getOrCreateSession(String userId, String sessionId) {
        LambdaQueryWrapper<TriageSession> query = new LambdaQueryWrapper<>();
        query.eq(TriageSession::getUserId, userId);
        query.eq(TriageSession::getSessionId, sessionId);
        TriageSession existing = triageSessionMapper.selectOne(query);
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
        triageSessionMapper.insert(created);
        return created;
    }

    private List<TriageTurn> listRecentTurns(String sessionId) {
        LambdaQueryWrapper<TriageTurn> query = new LambdaQueryWrapper<>();
        query.eq(TriageTurn::getSessionId, sessionId);
        query.orderByDesc(TriageTurn::getTurnNo);
        query.last("LIMIT " + MAX_HISTORY_TURNS);
        List<TriageTurn> desc = triageTurnMapper.selectList(query);
        if (desc.isEmpty()) {
            return Collections.emptyList();
        }
        List<TriageTurn> asc = new ArrayList<>(desc.size());
        for (int i = desc.size() - 1; i >= 0; i--) {
            asc.add(desc.get(i));
        }
        return asc;
    }

    private void saveTurn(
            String sessionId,
            int turnNo,
            String content,
            String normalizedContent,
            String reply) {
        TriageTurn turn = TriageTurn.builder()
                .sessionId(sessionId)
                .turnNo(turnNo)
                .userMessage(content)
                .normalizedQuery(normalizedContent)
                .intent("AI_ANALYZE")
                .stage("analysis")
                .replyText(reply)
                .build();
        triageTurnMapper.insert(turn);
    }

    private void updateSessionAfterTurn(
            TriageSession triageSession,
            int askRound,
            int invalidIncrement,
            PatientAuthPrincipal principal) {
        Integer invalidCount = triageSession.getInvalidAnswerCount();
        if (invalidCount == null) {
            invalidCount = 0;
        }
        triageSession.setAskRound(askRound);
        triageSession.setInvalidAnswerCount(invalidCount + invalidIncrement);
        triageSession.setCurrentStage("analysis");
        triageSession.setStatus("active");
        fillSessionProfileFromPatient(triageSession, principal);
        triageSessionMapper.updateById(triageSession);
    }

    private void fillSessionProfileFromPatient(TriageSession triageSession, PatientAuthPrincipal principal) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        if (patient == null) {
            return;
        }
        triageSession.setCity(trimToNull(patient.getResidentCity()));
        triageSession.setArea(trimToNull(patient.getArea()));
        triageSession.setPatientAge(patient.getPatientAge());
        triageSession.setPatientGender(patient.getPatientGender() == null ? null : patient.getPatientGender().getCode());
        TriagePrefer prefer = patient.getTriagePrefer();
        if (prefer != null) {
            triageSession.setNearby(TriagePrefer.NEARBY.equals(prefer) ? 1 : 0);
        }
    }

    private void upsertSlotState(String sessionId, List<TriageTurn> historyTurns, String latestContent) {
        List<String> symptoms = new ArrayList<>();
        for (int i = 0; i < historyTurns.size(); i++) {
            TriageTurn turn = historyTurns.get(i);
            if (StringUtils.hasText(turn.getUserMessage())) {
                symptoms.add(turn.getUserMessage().trim());
            }
        }
        symptoms.add(latestContent.trim());
        String symptomsJson = toJson(symptoms);

        LambdaQueryWrapper<TriageSlotState> query = new LambdaQueryWrapper<>();
        query.eq(TriageSlotState::getSessionId, sessionId);
        TriageSlotState existing = triageSlotStateMapper.selectOne(query);
        if (existing == null) {
            TriageSlotState created = TriageSlotState.builder()
                    .sessionId(sessionId)
                    .symptomsJson(symptomsJson)
                    .build();
            triageSlotStateMapper.insert(created);
            return;
        }
        existing.setSymptomsJson(symptomsJson);
        triageSlotStateMapper.updateById(existing);
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
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
}
