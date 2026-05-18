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
import com.intelligenthealthcare.patient.domain.model.Gender;
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
import java.io.IOException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.redisson.api.RList;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Service
/**
 * AI 导诊编排服务：
 * 1) 管理会话与轮次持久化；
 * 2) 接入 MCP 图片分析结果；
 * 3) 通过 ReAct 循环驱动工具调用（知识检索 / 医院推荐 / 急症预警 / 会话记忆 / 用户档案）并收敛最终答复。
 */
public class AiAnalysisServiceImpl implements AiAnalysisService {

    // ==================== 常量与规则配置 ====================
    // 这里集中维护缓存参数、信息抽取正则、回复约束与关键字词典。
    private static final int MAX_HISTORY_TURNS = 6;
    private static final long SESSION_CACHE_TTL_MINUTES = 20L;
    private static final String SESSION_HISTORY_CACHE_PREFIX = "ai:session:history:";
    private static final String APPEND_TURN_LUA = """
            redis.call('RPUSH', KEYS[1], ARGV[1])
            redis.call('LTRIM', KEYS[1], -tonumber(ARGV[2]), -1)
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
            return 1
            """;
    private static final int MAX_OBSERVATION_LENGTH = 1200;
    private static final Pattern AGE_PATTERN = Pattern.compile("(\\d{1,3})\\s*岁");
    private static final Pattern CITY_PATTERN = Pattern.compile("(?:在|住在|常驻|来自|城市(?:是|为)?)([\\u4e00-\\u9fa5]{2,12}(?:市|自治州|地区|盟)?)");
    private static final Pattern AREA_PATTERN = Pattern.compile("(?:在|住在|位于|区域|地区)([\\u4e00-\\u9fa5]{1,12}(?:区|县|市))");
    private static final List<String> SUPPLEMENTAL_INPUT_KEYWORDS = List.of(
            "补充", "另外", "还有", "此外", "更新", "刚刚", "刚才", "并且", "而且");
    private static final String FINAL_OUTPUT_RULES = """
            请仅输出面向患者可直接阅读的内容，禁止输出任何实现细节（如 MCP、ReAct、推理过程、模型步骤）。
            输出格式必须满足：
            1) 仅用纯文本，不要使用 Markdown 表格、代码块、分隔线（---）、emoji/图标符号（如 ✅⚠️▪️）。
            2) 建议使用三段短标题：
               1.关键信息提取
               2.初步风险提示
               3.建议下一步检查方向
            3) 结尾仅保留一句简短免责声明：这不是最终临床诊断。
            4) 推理顺序必须“先常见病、后少见病、再重症排查”。
            5) 在无明显红旗证据前，不要先给出“癌症/肿瘤”等高风险结论。
            6) 第一段需覆盖：已知线索、缺失信息、最可能方向。
            7) 第二段需区分：可观察风险与需立即就医信号。
            8) 第三段需提供：立刻可执行动作、观察时长、复诊触发条件。
            9) 每段控制在 7~9 条短句，通俗明确，避免空话与重复。
            10) 不要输出“可打印表格/继续生成”等收尾句。
            11) 若本轮用户输入属于补充信息，必须明确写出“本轮新增信息的变化点”。
            """;
    private static final String NON_MEDICAL_REPLY =
            "我当前仅支持医疗健康相关咨询，例如症状分析、就医建议、检查指标解读等。请描述您的健康问题，我会继续帮助您。";
    private static final List<String> MEDICAL_KEYWORDS = List.of(
            "症状", "发烧", "发热", "咳嗽", "头痛", "头晕", "腹痛", "腹泻", "呕吐", "恶心",
            "胸闷", "胸痛", "心慌", "呼吸", "咽喉", "皮疹", "红肿", "疼痛", "出血", "感染",
            "炎症", "血压", "血糖", "体温", "就医", "医院", "科室", "医生", "挂号", "急诊",
            "药", "用药", "检查", "化验", "检验", "报告", "医疗", "健康", "护理", "康复",
            "诊断", "治疗", "疾病", "病情");

    // ==================== 核心依赖注入 ====================
    // 包含 LLM、知识工具、仓储与缓存客户端等运行时依赖。
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
    private final TaskExecutor taskExecutor;
    private final int reactMaxSteps;
    private final int reactRagTopK;
    private final boolean reactDebugTrace;
    private final int maxVisionImages;
    private final int deepMaxVisionImages;

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
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            @Value("${app.agent.react.max-steps:4}") int reactMaxSteps,
            @Value("${app.agent.react.rag-top-k:3}") int reactRagTopK,
            @Value("${app.agent.react.debug-trace:false}") boolean reactDebugTrace,
            @Value("${app.vision.max-images:2}") int maxVisionImages,
            @Value("${app.vision.deep-max-images:5}") int deepMaxVisionImages) {
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
        this.taskExecutor = taskExecutor;
        this.reactMaxSteps = reactMaxSteps;
        this.reactRagTopK = reactRagTopK;
        this.reactDebugTrace = reactDebugTrace;
        this.maxVisionImages = maxVisionImages;
        this.deepMaxVisionImages = deepMaxVisionImages;
    }

    // ==================== 同步分析主流程 ====================
    // 非流式接口：执行完整分析并一次性返回最终结果。
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
            Double longitude,
            Boolean deepImageAnalysis) {
        boolean useDeepImageAnalysis = Boolean.TRUE.equals(deepImageAnalysis);
        List<String> safeImages = sanitizeImages(images, useDeepImageAnalysis);
        boolean hasImages = !safeImages.isEmpty();
        if (!StringUtils.hasText(content) && !hasImages) {
            return new AiAnalyzeResult(resolveSessionId(sessionId), "输入内容为空，无法分析。");
        }
        if (!isMedicalQuery(content, hasImages)) {
            return new AiAnalyzeResult(resolveSessionId(sessionId), NON_MEDICAL_REPLY, null);
        }

        String resolvedSessionId = resolveSessionId(sessionId);
        String userId = principal.getId().toString();
        TriageSession triageSession = getOrCreateSession(userId, resolvedSessionId);
        applyUserLocation(triageSession, latitude, longitude);
        Patient currentPatient = autoBackfillProfileIfMissing(principal.getId(), content);
        int turnNo = nextTurnNo(triageSession.getAskRound());
        List<TriageTurn> historyTurns = listRecentTurns(resolvedSessionId);

        // MCP 集成：通过 MCP ImageVisionTools 调用 DashScope 多模态模型分析图片
        String imageAnalysis = null;
        if (hasImages) {
            imageAnalysis = imageVisionTools.analyzeMedicalImages(
                        safeImages,
                        buildPatientProfileContext(principal) + "\n症状描述：" + content,
                        safeImages.size());
        }

        String normalizedContent = normalizeKey(content);
        try {
            // ReAct 主循环：模型按 step 决策 action，直到 FINISH 或达到最大步数。
            ReActResult reactResult = runReActAnalysis(principal, resolvedSessionId, content,
                    hasImages, imageAnalysis, historyTurns);
            String result = enrichGuidedFollowUp(reactResult.finalAnswer(), content, hasImages, currentPatient);
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

    // ==================== 流式分析主流程 ====================
    // 使用 SSE 按阶段推送状态与文本 chunk，前端可实时展示。
    /**
     * ReAct 流式分析：通过 SseEmitter 逐条推送推理状态与最终答复的逐 token 生成。
     * <p>
     * <b>为什么用 SseEmitter 而非 Flux</b>：Spring MVC 对 Flux 的适配层存在内部缓冲，
     * 直到流结束才 flush；SseEmitter 基于 Servlet 3.1 异步，每次 send() 立即刷入
     * TCP 缓冲区，实现真正的逐条输出。
     * <p>
     * <b>线程模型</b>：请求线程仅创建 SseEmitter 并提交到 TaskExecutor 即释放；
     * 全部 AI 调用与 DB 写入均在 Worker 线程内顺序执行，避免阻塞 Tomcat 线程池。
     * <p>
     * SSE 事件：
     * <ul>
     * <li>{@code status} — 推理进度描述</li>
     * <li>{@code chunk}  — 文本片段，逐 token 到达</li>
     * <li>{@code done}   — 含 {@code sessionId} 与 {@code imageAnalysis} 的 JSON</li>
     * <li>{@code error}  — 错误消息</li>
     * </ul>
     */
    @Override
    public SseEmitter analyzeStream(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images,
            Double latitude,
            Double longitude,
            Boolean deepImageAnalysis) {

        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时，防止半开连接泄漏
        String resolvedSessionId = resolveSessionId(sessionId);
        AtomicBoolean streamAvailable = new AtomicBoolean(true);
        emitter.onCompletion(() -> streamAvailable.set(false));
        emitter.onTimeout(() -> streamAvailable.set(false));
        emitter.onError(ex -> streamAvailable.set(false));

        // 全部业务逻辑在 Worker 线程中执行，不占用 Tomcat 请求线程
        taskExecutor.execute(() -> {
            try {
                boolean useDeepImageAnalysis = Boolean.TRUE.equals(deepImageAnalysis);
                List<String> safeImages = sanitizeImages(images, useDeepImageAnalysis);
                boolean hasImages = !safeImages.isEmpty();
                // --- Phase 1: validation ---
                if (!StringUtils.hasText(content) && !hasImages) {
                    safeSend(emitter, statusEvent("输入内容为空，无法分析。"));
                    safeSend(emitter, doneEvent(resolvedSessionId, null));
                    emitter.complete();
                    return;
                }
                if (!isMedicalQuery(content, hasImages)) {
                    safeSend(emitter, statusEvent(NON_MEDICAL_REPLY));
                    safeSend(emitter, chunkEvent(NON_MEDICAL_REPLY));
                    safeSend(emitter, doneEvent(resolvedSessionId, null));
                    emitter.complete();
                    return;
                }

                // --- Phase 2: session & user setup ---
                String userId = principal.getId().toString();
                TriageSession triageSession = getOrCreateSession(userId, resolvedSessionId);
                applyUserLocation(triageSession, latitude, longitude);
                Patient currentPatient = autoBackfillProfileIfMissing(principal.getId(), content);
                int turnNo = nextTurnNo(triageSession.getAskRound());
                List<TriageTurn> historyTurns = listRecentTurns(resolvedSessionId);
                String normalizedContent = normalizeKey(content);

                // --- Phase 3: MCP image analysis ---
                String imageAnalysis = null;
                if (hasImages) {
                    if (streamAvailable.get() && !safeSend(emitter, statusEvent("正在分析上传的图片..."))) {
                        streamAvailable.set(false);
                    }
                    imageAnalysis = imageVisionTools.analyzeMedicalImages(
                            safeImages,
                            buildPatientProfileContext(principal) + "\n症状描述：" + content,
                            safeImages.size());
                    if (streamAvailable.get() && !safeSend(emitter, statusEvent("图片分析完成"))) {
                        streamAvailable.set(false);
                    }
                }

                // --- Phase 4: ReAct loop ---
                StringBuilder scratchpad = new StringBuilder();
                String finalPrompt = null;
                String draftAnswer = null;
                for (int step = 1; step <= reactMaxSteps; step++) {
                    if (streamAvailable.get()
                            && !safeSend(emitter, statusEvent("正在推理分析...（步骤 " + step + "/" + reactMaxSteps + "）"))) {
                        streamAvailable.set(false);
                    }
                    ReActDecision decision = planNextAction(principal, resolvedSessionId, content,
                            hasImages, imageAnalysis, historyTurns, scratchpad, step);
                    String action = normalizeAction(decision.action());

                    if ("FINISH".equals(action)) {
                        draftAnswer = decision.finalAnswer();
                        if (!StringUtils.hasText(draftAnswer)) {
                            finalPrompt = buildFinalPrompt(content, hasImages, imageAnalysis,
                                    historyTurns, scratchpad);
                        }
                        break;
                    }

                    String observation = executeAction(action, decision.actionInput(), principal,
                            resolvedSessionId, content, historyTurns,
                            triageSessionRepository.findBySessionId(resolvedSessionId).orElse(null));
                    appendScratchpad(scratchpad, step, decision, observation);
                }
                if (scratchpad.length() > 0 && finalPrompt == null && !StringUtils.hasText(draftAnswer)) {
                    finalPrompt = buildFinalPrompt(content, hasImages, imageAnalysis, historyTurns, scratchpad);
                }

                // --- Phase 5: stream answer ---
                // 根据来源选择迭代器：
                //   directAnswer  → 将预生成文本按 4 字符切块，延迟 20ms 模拟流式
                //   finalPrompt   → ChatClient.stream().toIterable() 在迭代时自然阻塞
                //                   等待 LLM 的下一个 token（WebClient 异步回调驱动）
                //   其他          → 兜底文本
                String finalImageAnalysis = imageAnalysis;
                boolean directAnswer = StringUtils.hasText(draftAnswer);
                Iterable<String> answerIterable;
                if (directAnswer) {
                    answerIterable = chunkedIterable(draftAnswer.trim(), 4);
                } else if (finalPrompt != null) {
                    // toIterable() 返回 BlockingIterable：hasNext() 阻塞直到 token 到达或流结束
                    answerIterable = chatClient.prompt(finalPrompt).stream().content().toIterable();
                } else {
                    answerIterable = List.of("分析结束，请查看上述建议。");
                }

                // 遍历 token 并逐条推送到客户端；保留完整文本用于末尾持久化
                StringBuilder fullAnswer = new StringBuilder();
                try {
                    for (String chunk : answerIterable) {
                        fullAnswer.append(chunk);
                        if (streamAvailable.get() && !safeSend(emitter, chunkEvent(chunk))) {
                            streamAvailable.set(false);
                        }
                        if (directAnswer) {
                            // 预生成文本没有自然延迟，加 20ms 让前端有逐字出现的感觉
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } catch (RuntimeException streamErr) {
                    // LLM 流异常：记录部分答复
                    String errorText = streamErr.getMessage() == null ? "未知错误" : streamErr.getMessage();
                    String partialAnswer = fullAnswer.length() > 0 ? fullAnswer.toString() : "分析失败：" + errorText;
                    persistFailed(content, normalizedContent, safeImages, finalImageAnalysis, scratchpad.toString(),
                            errorText, partialAnswer, currentPatient, resolvedSessionId, turnNo, triageSession, principal);
                    if (streamAvailable.get()) {
                        safeSend(emitter, errorEvent("分析失败：" + errorText));
                    }
                    completeQuietly(emitter);
                    return;
                }

                // --- Phase 6: persist & done ---
                String result = fullAnswer.toString();
                if (!StringUtils.hasText(result)) {
                    result = "模型未生成有效答复，请重试。";
                }
                result = enrichGuidedFollowUp(result, content, hasImages, currentPatient);
                String rawDecisionJson = buildTurnDecisionJson(content, normalizedContent,
                        safeImages, finalImageAnalysis, scratchpad.toString(), "SUCCESS", null);
                auditApplicationService.recordAiAnalyzeSuccess(content, safeImages, result, currentPatient, resolvedSessionId);
                saveTurn(resolvedSessionId, turnNo, content, normalizedContent, result, rawDecisionJson);
                updateSessionAfterTurn(triageSession, turnNo, 0, principal, result);
                upsertSlotState(resolvedSessionId, historyTurns, content);
                if (streamAvailable.get()) {
                    safeSend(emitter, doneEvent(resolvedSessionId, finalImageAnalysis));
                }
                completeQuietly(emitter);
            } catch (RuntimeException ex) {
                String errorText = ex.getMessage() == null ? "未知错误" : ex.getMessage();
                if (streamAvailable.get()) {
                    safeSend(emitter, errorEvent(errorText));
                    safeSend(emitter, doneEvent(resolvedSessionId, null));
                }
                completeQuietly(emitter);
            }
        });

        return emitter;
    }

    // ==================== 会话查询接口 ====================
    // 提供“会话列表”和“单会话历史内容”查询能力。
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

    // ==================== Prompt 与 ReAct 规划 ====================
    // 负责构建提示词、规划动作、解析模型决策并收敛最终回答。
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
                请遵循：先从常见病因解释，再补充少见病因，最后才讨论重症排查。
                若没有明确红旗证据，不要直接给出癌症/肿瘤倾向结论。
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
                  "action": "SEARCH_MEDICAL_KNOWLEDGE|RECOMMEND_HOSPITAL|RECOMMEND_DOCTOR|LOG_EMERGENCY|SESSION_MEMORY|PATIENT_PROFILE|FINISH",
                  "actionInput": "动作输入，可为空字符串",
                  "finalAnswer": "仅当 action=FINISH 时填写，其他时候留空"
                }
                规则：
                1) 若信息不足，优先用 SEARCH_MEDICAL_KNOWLEDGE 或 SESSION_MEMORY。
                2) 若用户要求就近就医推荐，优先用 RECOMMEND_HOSPITAL。
                2.1) 若用户明确要求推荐具体医生，优先用 RECOMMEND_DOCTOR。
                3) 若识别为疑似急症，先执行 LOG_EMERGENCY 后再给建议。
                4) 最多几步后必须 FINISH，给出结构化医疗建议并提示"非临床诊断"。
                5) 不要编造检查结果，不要输出危言耸听结论。
                6) 疾病推理顺序：先常见病，再少见病，最后再考虑重症或肿瘤可能。
                7) 若缺乏红旗证象，不要在结论中优先提“癌症/肿瘤”。
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

    // ==================== 动作执行与工具观察 ====================
    // 将 ReAct 动作路由到知识检索、医院/医生推荐、急症日志等工具。
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
        if ("RECOMMEND_DOCTOR".equals(action)) {
            return observeDoctorRecommendation(principal, actionInput, latestContent, triageSession);
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

    private String observeDoctorRecommendation(
            PatientAuthPrincipal principal,
            String actionInput,
            String latestContent,
            TriageSession triageSession) {
        Patient patient = patientRepository.findById(principal.getId()).orElse(null);
        String city = patient == null ? "" : patient.getResidentCity();
        String area = patient == null ? "" : patient.getArea();
        String symptomSummary = StringUtils.hasText(actionInput) ? actionInput.trim() : latestContent.trim();
        BigDecimal latitude = triageSession == null ? null : triageSession.getLatitude();
        BigDecimal longitude = triageSession == null ? null : triageSession.getLongitude();
        String toolResult = medicalDecisionTools.recommendDoctor(city, area, symptomSummary, latitude, longitude, 3);
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

    // ==================== 档案与会话文案辅助 ====================
    // 提供患者画像拼装、会话标题生成与回答后处理能力。
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
                + FINAL_OUTPUT_RULES;
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
                || "RECOMMEND_DOCTOR".equals(normalized)
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

    private boolean isMedicalQuery(String content, boolean hasImages) {
        // 用户上传了图片时，允许进入医疗分析流程，避免“文本过短/无关键词”被误拦截。
        if (hasImages) {
            return true;
        }
        if (!StringUtils.hasText(content)) {
            return false;
        }
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

    // ==================== 会话历史缓存（Redis） ====================
    // 读取/写入最近轮次，降低数据库压力并保持上下文连续性。
    private List<TriageTurn> readRecentTurnsFromCache(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Collections.emptyList();
        }
        try {
            RList<String> list = redissonClient.getList(historyCacheKey(sessionId), StringCodec.INSTANCE);
            List<String> rows = list.readAll();
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<TriageTurn> result = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                String row = rows.get(i);
                if (!StringUtils.hasText(row)) {
                    continue;
                }
                CachedTurn item = objectMapper.readValue(row, CachedTurn.class);
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
        int start = Math.max(turns.size() - MAX_HISTORY_TURNS, 0);
        List<String> rows = new ArrayList<>();
        try {
            for (int i = start; i < turns.size(); i++) {
                TriageTurn turn = turns.get(i);
                CachedTurn cachedTurn = new CachedTurn(turn.getTurnNo(), turn.getUserMessage(), turn.getReplyText());
                rows.add(writeCachedTurn(cachedTurn));
            }
        } catch (JsonProcessingException ex) {
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        writeCachedTurns(sessionId, rows);
    }

    private void appendTurnToCache(String sessionId, TriageTurn turn) {
        if (!StringUtils.hasText(sessionId) || turn == null) {
            return;
        }
        try {
            CachedTurn cachedTurn = new CachedTurn(turn.getTurnNo(), turn.getUserMessage(), turn.getReplyText());
            String row = writeCachedTurn(cachedTurn);
            redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    APPEND_TURN_LUA,
                    RScript.ReturnType.INTEGER,
                    List.of(historyCacheKey(sessionId)),
                    row,
                    String.valueOf(MAX_HISTORY_TURNS),
                    String.valueOf(SESSION_CACHE_TTL_MINUTES * 60));
        } catch (RuntimeException | JsonProcessingException ex) {
            // 缓存写入失败不影响主流程
        }
    }

    private void writeCachedTurns(String sessionId, List<String> rows) {
        try {
            RList<String> list = redissonClient.getList(historyCacheKey(sessionId), StringCodec.INSTANCE);
            list.delete();
            list.addAll(rows);
            list.expire(Duration.ofMinutes(SESSION_CACHE_TTL_MINUTES));
        } catch (RuntimeException ex) {
            // 缓存写入失败不影响主流程
        }
    }

    private String writeCachedTurn(CachedTurn turn) throws JsonProcessingException {
        return objectMapper.writeValueAsString(turn);
    }

    private String historyCacheKey(String sessionId) {
        return SESSION_HISTORY_CACHE_PREFIX + sessionId.trim();
    }

    // ==================== 输入清洗与会话编号 ====================
    // 对图片输入限流、规范化 sessionId 与轮次。
    private List<String> sanitizeImages(List<String> images, boolean deepImageAnalysis) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        int configuredLimit = deepImageAnalysis ? deepMaxVisionImages : maxVisionImages;
        int limit = Math.max(configuredLimit, 1);
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

    // ==================== 用户资料自动回填 ====================
    // 从自然语言中提取年龄/性别/城市/区域/偏好，仅补齐缺失字段。
    /**
     * 对话中提及档案信息时自动回填“缺失字段”，已存在字段不覆盖。
     * 目标：减少用户跨会话重复输入，提高后续推荐稳定性。
     */
    private Patient autoBackfillProfileIfMissing(Long patientId, String content) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null || !StringUtils.hasText(content)) {
            return patient;
        }

        boolean changed = false;
        Integer inferredAge = inferAge(content);
        if (patient.getPatientAge() == null && inferredAge != null) {
            patient.setPatientAge(inferredAge);
            changed = true;
        }

        Gender inferredGender = inferGender(content);
        if (inferredGender != null && (patient.getPatientGender() == null || patient.getPatientGender() == Gender.UNKNOWN)) {
            patient.setPatientGender(inferredGender);
            changed = true;
        }

        String inferredCity = inferCity(content);
        if (StringUtils.hasText(inferredCity) && !StringUtils.hasText(patient.getResidentCity())) {
            patient.setResidentCity(inferredCity);
            changed = true;
        }

        String inferredArea = inferArea(content);
        if (StringUtils.hasText(inferredArea) && !StringUtils.hasText(patient.getArea())) {
            patient.setArea(inferredArea);
            changed = true;
        }

        TriagePrefer inferredPrefer = inferTriagePrefer(content);
        if (inferredPrefer != null && (patient.getTriagePrefer() == null || patient.getTriagePrefer() == TriagePrefer.NEARBY)) {
            patient.setTriagePrefer(inferredPrefer);
            changed = true;
        }

        if (!changed) {
            return patient;
        }
        return patientRepository.save(patient);
    }

    private Integer inferAge(String content) {
        Matcher matcher = AGE_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        try {
            int age = Integer.parseInt(matcher.group(1));
            if (age < 1 || age > 120) {
                return null;
            }
            return age;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Gender inferGender(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        boolean male = normalized.contains("男性") || normalized.contains("男生") || normalized.contains("男");
        boolean female = normalized.contains("女性") || normalized.contains("女生") || normalized.contains("女");
        if (male && !female) {
            return Gender.MALE;
        }
        if (female && !male) {
            return Gender.FEMALE;
        }
        return null;
    }

    private String inferCity(String content) {
        Matcher matcher = CITY_PATTERN.matcher(content);
        if (matcher.find()) {
            return normalizeCity(matcher.group(1));
        }
        if (content.contains("北京")) return "北京市";
        if (content.contains("上海")) return "上海市";
        if (content.contains("天津")) return "天津市";
        if (content.contains("重庆")) return "重庆市";
        return null;
    }

    private String normalizeCity(String rawCity) {
        if (!StringUtils.hasText(rawCity)) {
            return null;
        }
        String city = rawCity.trim();
        if (city.endsWith("市") || city.endsWith("自治州") || city.endsWith("地区") || city.endsWith("盟")) {
            return city;
        }
        if ("北京".equals(city) || "上海".equals(city) || "天津".equals(city) || "重庆".equals(city)) {
            return city + "市";
        }
        return city;
    }

    private String inferArea(String content) {
        Matcher matcher = AREA_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String area = matcher.group(1);
        return StringUtils.hasText(area) ? area.trim() : null;
    }

    private TriagePrefer inferTriagePrefer(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        if (normalized.contains("权威优先") || normalized.contains("专家优先") || normalized.contains("三甲优先")) {
            return TriagePrefer.AUTHORITY;
        }
        if (normalized.contains("紧急优先") || normalized.contains("急症优先") || normalized.contains("先急诊")) {
            return TriagePrefer.EMERGENCY;
        }
        if (normalized.contains("就近就医") || normalized.contains("就近优先") || normalized.contains("附近医院")) {
            return TriagePrefer.NEARBY;
        }
        return null;
    }

    // ==================== 追问引导与差异化输出 ====================
    // 补充信息场景下生成“变化点”并按优先级追问关键信息。
    /**
     * 当用户只给出模糊补充（如“有过往病史”）时，自动追加追问式引导，避免回复重复且无推进。
     */
    private String enrichGuidedFollowUp(String answer, String latestContent, boolean hasImages, Patient currentPatient) {
        String normalizedAnswer = defaultText(answer);
        if (!StringUtils.hasText(normalizedAnswer)) {
            return normalizedAnswer;
        }
        String result = ensureDeltaPointIfSupplemental(normalizedAnswer, latestContent);
        String priorityFollowUp = buildPriorityFollowUpQuestion(latestContent, currentPatient, hasImages);
        if (StringUtils.hasText(priorityFollowUp) && !result.contains(priorityFollowUp)) {
            StringBuilder sb = new StringBuilder(result);
            sb.append("\n\n下一步请优先补充：");
            sb.append("\n- ").append(priorityFollowUp);
            result = sb.toString();
        }
        if (shouldGuideMissingProfile(currentPatient, result)) {
            StringBuilder sb = new StringBuilder(result);
            sb.append("\n\n为提升判断准确性，请补充：");
            if (currentPatient == null || currentPatient.getPatientAge() == null) {
                sb.append("\n- 年龄");
            }
            if (currentPatient == null || currentPatient.getPatientGender() == null || currentPatient.getPatientGender() == Gender.UNKNOWN) {
                sb.append("\n- 性别");
            }
            result = sb.toString();
        }
        return normalizeFinalResponse(result);
    }

    private boolean isSupplementalInput(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim();
        for (int i = 0; i < SUPPLEMENTAL_INPUT_KEYWORDS.size(); i++) {
            if (normalized.contains(SUPPLEMENTAL_INPUT_KEYWORDS.get(i))) {
                return true;
            }
        }
        return normalized.length() <= 30 && (normalized.contains("病史") || normalized.contains("还是") || normalized.contains("不确定"));
    }

    private String ensureDeltaPointIfSupplemental(String answer, String latestContent) {
        if (!isSupplementalInput(latestContent)) {
            return answer;
        }
        if (answer.contains("变化点") || answer.contains("相比上一轮") || answer.contains("本轮新增")) {
            return answer;
        }
        return "本轮新增信息的变化点："
                + buildDeltaSummary(latestContent)
                + "\n"
                + answer;
    }

    private String buildDeltaSummary(String latestContent) {
        String text = defaultText(latestContent);
        if (text.contains("病史")) {
            return "已将既往病史纳入判断，当前优先排查是否为既往相关复发。";
        }
        if (text.contains("加重") || text.contains("更疼") || text.contains("严重")) {
            return "症状严重度较上一轮上调，风险评估同步提高。";
        }
        if (text.contains("发热") || text.contains("呕吐") || text.contains("无力") || text.contains("视物")) {
            return "新增可能红旗线索，需优先排查急症风险。";
        }
        return "已基于本轮补充信息调整风险判断与下一步建议。";
    }

    /**
     * 只要用户提到病史但未给出具体信息（或表达不确定），就强制触发病史追问。
     */
    private boolean shouldForceHistoryClarification(String text) {
        if (!StringUtils.hasText(text) || !text.contains("病史")) {
            return false;
        }
        if (text.contains("不确定") || text.contains("不清楚") || text.contains("不太确定")
                || text.contains("不记得") || text.contains("是不是") || text.contains("是否是")) {
            return true;
        }
        // 仅泛泛提到“病史”，但未包含关键细节字段时，也应继续追问。
        boolean hasDetail = text.contains("确诊")
                || text.contains("几年")
                || text.contains("发作")
                || text.contains("复发")
                || text.contains("用药")
                || text.contains("检查");
        return !hasDetail;
    }

    /**
     * 追问优先级策略：每轮只追问最高优先级缺失信息，避免一次性追问过多内容。
     */
    private String buildPriorityFollowUpQuestion(String latestContent, Patient currentPatient, boolean hasImages) {
        String text = defaultText(latestContent);
        boolean hasRedFlagInfo = containsAny(text, "发热", "呕吐", "肢体无力", "言语不清", "视物模糊", "抽搐", "意识");
        boolean hasCoreSymptomDetail = containsAny(text, "持续", "多久", "几天", "几小时", "部位", "左侧", "右侧", "跳痛", "胀痛", "压痛");
        boolean hasMedicationInfo = containsAny(text, "用药", "吃了", "服用", "布洛芬", "止痛药", "阿司匹林", "对乙酰氨基酚");
        boolean historyUnclear = shouldForceHistoryClarification(text);

        // 病史不确定时，优先追问病史细节（用户明确反馈的核心诉求）。
        if (historyUnclear) {
            return "请说明既往病史名称、确诊时间、最近一次发作时间，以及这次是否与既往相似。";
        }
        if (!hasRedFlagInfo) {
            return "是否出现突发最剧烈头痛、持续呕吐、肢体无力/言语不清或发热颈僵？如有请立即急诊。";
        }
        if (!hasCoreSymptomDetail) {
            return "本次症状持续多久、主要在什么部位、疼痛性质是胀痛还是跳痛？";
        }
        if (!hasMedicationInfo) {
            return "这次发作后是否已用药？请补充药名、剂量和效果。";
        }
        if (!hasImages) {
            return "如方便可上传检查单或相关图片（检验报告/影像结论），可帮助更快判断。";
        }
        if (currentPatient == null || currentPatient.getPatientAge() == null) {
            return "请补充年龄，便于更准确评估风险分层。";
        }
        if (currentPatient.getPatientGender() == null || currentPatient.getPatientGender() == Gender.UNKNOWN) {
            return "请补充性别，便于更准确评估相关疾病可能性。";
        }
        return "";
    }

    private boolean shouldGuideMissingProfile(Patient patient, String answer) {
        if (answer.contains("请补充") && (answer.contains("年龄") || answer.contains("性别"))) {
            return false;
        }
        if (patient == null) {
            return true;
        }
        boolean missingAge = patient.getPatientAge() == null;
        boolean missingGender = patient.getPatientGender() == null || patient.getPatientGender() == Gender.UNKNOWN;
        return missingAge || missingGender;
    }

    /**
     * 最终答复压缩器：避免单轮输出过长，统一为三段核心 + 一句免责声明。
     */
    private String normalizeFinalResponse(String text) {
        String normalized = defaultText(text)
                .replace("【结构化医疗建议】", "")
                .replace("\r\n", "\n");
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }

        String[] rawLines = normalized.split("\n");
        List<String> cleaned = new ArrayList<>();
        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i].trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (line.contains("非临床诊断") || line.contains("最终临床诊断") || line.contains("不替代面诊")) {
                continue;
            }
            cleaned.add(line);
        }
        List<String> followUpLines = extractPriorityFollowUpLines(cleaned);

        List<String> output = new ArrayList<>();
        int currentSection = 0;
        int sectionLineCount = 0;
        for (int i = 0; i < cleaned.size(); i++) {
            String line = cleaned.get(i);
            boolean isSectionTitle = line.matches("^[1-3][\\.、].*");
            if (isSectionTitle) {
                currentSection++;
                if (currentSection > 3) {
                    break;
                }
                sectionLineCount = 1;
                output.add(line);
                continue;
            }
            if (currentSection == 0) {
                // 没有标题时按默认摘要处理，最多保留 12 行
                if (output.size() < 12) {
                    output.add(line);
                }
                continue;
            }
            if (sectionLineCount < 6) { // 标题+最多5行内容
                output.add(line);
                sectionLineCount++;
            }
        }

        StringBuilder sb = new StringBuilder(String.join("\n", output));
        if (!followUpLines.isEmpty() && !sb.toString().contains("下一步请优先补充")) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(String.join("\n", followUpLines));
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append("这不是最终临床诊断。");
        return sb.toString();
    }

    private List<String> extractPriorityFollowUpLines(List<String> cleaned) {
        List<String> lines = new ArrayList<>();
        boolean capturing = false;
        for (int i = 0; i < cleaned.size(); i++) {
            String line = cleaned.get(i);
            if (line.startsWith("下一步请优先补充")) {
                capturing = true;
                lines.add(line);
                continue;
            }
            if (!capturing) {
                continue;
            }
            if (line.startsWith("- ")) {
                lines.add(line);
                if (lines.size() >= 3) {
                    break;
                }
                continue;
            }
            break;
        }
        return lines;
    }

    // ==================== 轻量内部模型 ====================
    // ReAct 决策/结果与缓存轮次的内部记录类型。
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

    // ---- SSE helpers ----

    /**
     * 安全发送 SSE 事件。
     * IOException 表示客户端已断开（如关闭标签页），此时返回 false 让外层终止循环。
     */
    private static boolean safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            return false; // 客户端断开
        } catch (RuntimeException e) {
            return false; // 异步请求已结束等场景
        }
    }

    private static void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // 连接已断开时 complete 可能抛异常，忽略即可
        }
    }

    private static SseEmitter.SseEventBuilder statusEvent(String message) {
        return SseEmitter.event().name("status").data(message);
    }

    private static SseEmitter.SseEventBuilder chunkEvent(String text) {
        return SseEmitter.event().name("chunk").data(text);
    }

    /**
     * 构造 done 事件的 JSON payload。
     * 手动拼接 JSON 避免引入额外序列化依赖，注意 imageAnalysis 可能含特殊字符需要转义。
     */
    private static SseEmitter.SseEventBuilder doneEvent(String sessionId, String imageAnalysis) {
        String json = "{\"sessionId\":\"" + (sessionId == null ? "" : sessionId) + "\""
                + ",\"imageAnalysis\":" + (imageAnalysis == null ? "null" : "\"" + escapeJson(imageAnalysis) + "\"")
                + "}";
        return SseEmitter.event().name("done").data(json);
    }

    private static SseEmitter.SseEventBuilder errorEvent(String message) {
        return SseEmitter.event().name("error").data(message);
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 将文本切分为定长块的可迭代视图，每次迭代按需取下一段。
     * 惰性求值，不会一次性创建全部字符串对象，适合长文本的渐进输出。
     */
    private static Iterable<String> chunkedIterable(String text, int chunkSize) {
        if (!StringUtils.hasText(text)) return List.of("");
        return () -> new java.util.Iterator<>() {
            private int pos = 0;
            @Override public boolean hasNext() { return pos < text.length(); }
            @Override public String next() {
                int end = Math.min(pos + chunkSize, text.length());
                String chunk = text.substring(pos, end);
                pos = end;
                return chunk;
            }
        };
    }

    /**
     * 流失败时持久化错误轮次，确保即使 LLM 中途出错也能保留部分答复与审计记录。
     * 内部异常静默吞掉，避免覆盖原始流错误。
     */
    private void persistFailed(
            String content, String normalizedContent, List<String> safeImages,
            String imageAnalysis, String trace, String errorText, String partialAnswer,
            Patient currentPatient, String sessionId, int turnNo,
            TriageSession triageSession, PatientAuthPrincipal principal) {
        try {
            String rawDecisionJson = buildTurnDecisionJson(content, normalizedContent,
                    safeImages, imageAnalysis, trace, "FAILED", errorText);
            auditApplicationService.recordAiAnalyzeFailed(content, safeImages, errorText, currentPatient, sessionId);
            saveTurn(sessionId, turnNo, content, normalizedContent, partialAnswer, rawDecisionJson);
            updateSessionAfterTurn(triageSession, turnNo, 1, principal, partialAnswer);
        } catch (RuntimeException ignored) {
            // 错误记录失败不影响主流程
        }
    }

    /** 为流式方法构建最终答复 prompt */
    private String buildFinalPrompt(
            String content, boolean hasImages, String imageAnalysis,
            List<TriageTurn> historyTurns, StringBuilder scratchpad) {
        return buildPrompt(content, hasImages, imageAnalysis, historyTurns)
                + "\n\n以下是 ReAct 过程中的关键信息，请据此给出最终答复：\n"
                + scratchpad
                + "\n\n输出要求：\n"
                + FINAL_OUTPUT_RULES;
    }
}
