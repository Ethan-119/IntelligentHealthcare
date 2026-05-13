package com.intelligenthealthcare.triage.application;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.triage.application.dto.AiAnalyzeResult;
import java.util.List;

public interface AiAnalysisService {

    AiAnalyzeResult analyze(PatientAuthPrincipal principal, String sessionId, String content, List<String> images);
}
