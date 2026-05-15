package com.intelligenthealthcare.triage.api;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.triage.api.dto.AiAnalysisRequest;
import com.intelligenthealthcare.triage.api.dto.AiAnalysisResponse;
import com.intelligenthealthcare.triage.api.dto.AiSessionConversationResponse;
import com.intelligenthealthcare.triage.api.dto.AiSessionSummaryResponse;
import com.intelligenthealthcare.triage.api.dto.AiSessionTurnResponse;
import com.intelligenthealthcare.triage.application.AiAnalysisService;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.triage.application.dto.AiSessionConversation;
import com.intelligenthealthcare.triage.application.dto.AiSessionSummary;
import com.intelligenthealthcare.triage.application.dto.AiSessionTurn;
import com.intelligenthealthcare.shared.security.CurrentPatient;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public AiAnalysisResponse analyze(
            @CurrentPatient PatientAuthPrincipal principal,
            @RequestBody AiAnalysisRequest request) {
        AiAnalyzeResult result = aiAnalysisService.analyze(
                principal,
                request.getSessionId(),
                request.getContent(),
                request.getImages());
        return AiAnalysisResponse.builder()
                .sessionId(result.sessionId())
                .result(result.result())
                .imageAnalysis(result.imageAnalysis())
                .build();
    }

    @GetMapping("/sessions")
    public List<AiSessionSummaryResponse> listSessions(@CurrentPatient PatientAuthPrincipal principal) {
        List<AiSessionSummary> sessions = aiAnalysisService.listMySessions(principal);
        List<AiSessionSummaryResponse> result = new ArrayList<>(sessions.size());
        for (int i = 0; i < sessions.size(); i++) {
            AiSessionSummary item = sessions.get(i);
            result.add(AiSessionSummaryResponse.builder()
                    .sessionId(item.sessionId())
                    .title(item.title())
                    .askRound(item.askRound())
                    .updateTime(item.updateTime())
                    .build());
        }
        return result;
    }

    @GetMapping("/sessions/{sessionId}/turns")
    public AiSessionConversationResponse getSessionConversation(
            @CurrentPatient PatientAuthPrincipal principal,
            @PathVariable("sessionId") String sessionId) {
        AiSessionConversation conversation = aiAnalysisService.getSessionConversation(principal, sessionId);
        List<AiSessionTurn> turns = conversation.turns();
        List<AiSessionTurnResponse> result = new ArrayList<>(turns.size());
        for (int i = 0; i < turns.size(); i++) {
            AiSessionTurn turn = turns.get(i);
            result.add(AiSessionTurnResponse.builder()
                    .turnNo(turn.turnNo())
                    .userMessage(turn.userMessage())
                    .replyText(turn.replyText())
                    .createTime(turn.createTime())
                    .build());
        }
        return AiSessionConversationResponse.builder()
                .sessionId(conversation.sessionId())
                .turns(result)
                .build();
    }
}
