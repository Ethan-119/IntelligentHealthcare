package com.intelligenthealthcare.triage.application;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import com.intelligenthealthcare.triage.application.dto.AiSessionConversation;
import com.intelligenthealthcare.triage.application.dto.AiSessionSummary;
import java.util.List;

public interface AiAnalysisService {

    AiAnalyzeResult analyze(
            PatientAuthPrincipal principal,
            String sessionId,
            String content,
            List<String> images,
            Double latitude,
            Double longitude);

    List<AiSessionSummary> listMySessions(PatientAuthPrincipal principal);

    AiSessionConversation getSessionConversation(PatientAuthPrincipal principal, String sessionId);
}
