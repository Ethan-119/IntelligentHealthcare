package com.intelligenthealthcare.triage.api;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.triage.api.dto.AiAnalysisRequest;
import com.intelligenthealthcare.triage.api.dto.AiAnalysisResponse;
import com.intelligenthealthcare.triage.application.AiAnalysisService;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.shared.security.CurrentPatient;
import lombok.RequiredArgsConstructor;
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
                .build();
    }
}
