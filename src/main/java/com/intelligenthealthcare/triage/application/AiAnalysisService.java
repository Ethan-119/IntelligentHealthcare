package com.intelligenthealthcare.triage.application;

import java.util.List;

public interface AiAnalysisService {

    String analyze(String content, List<String> images);
}
