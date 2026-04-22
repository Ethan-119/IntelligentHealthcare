package com.intelligenthealthcare.triage.api;

import com.intelligenthealthcare.triage.api.dto.AiAnalysisRequest;
import com.intelligenthealthcare.triage.api.dto.AiAnalysisResponse;
import com.intelligenthealthcare.triage.application.AiAnalysisService;
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
    public AiAnalysisResponse analyze(@RequestBody AiAnalysisRequest request) {
        String result = aiAnalysisService.analyze(request.getContent());
        return AiAnalysisResponse.builder().result(result).build();
    }
}
