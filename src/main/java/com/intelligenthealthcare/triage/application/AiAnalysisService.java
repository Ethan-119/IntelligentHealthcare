package com.intelligenthealthcare.triage.application;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.triage.application.dto.AiSessionConversation;
import com.intelligenthealthcare.triage.application.dto.AiSessionSummary;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiAnalysisService {

    AiAnalyzeResult analyze(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images,
            Double latitude,
            Double longitude,
            Boolean deepImageAnalysis);

    /**
     * ReAct 流式分析：通过 SseEmitter 逐条推送状态、token 片段与完成事件。
     * <p>
     * 事件类型：
     * <ul>
     * <li>{@code status}  — 推理进度描述</li>
     * <li>{@code chunk}   — 最终答复的文本片段，逐 token 到达</li>
     * <li>{@code done}    — 含 {@code sessionId} 与 {@code imageAnalysis} 的 JSON</li>
     * <li>{@code error}   — 错误消息</li>
     * </ul>
     */
    SseEmitter analyzeStream(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images,
            Double latitude,
            Double longitude,
            Boolean deepImageAnalysis);

    List<AiSessionSummary> listMySessions(PatientAuthPrincipal principal);

    AiSessionConversation getSessionConversation(PatientAuthPrincipal principal, String sessionId);
}
