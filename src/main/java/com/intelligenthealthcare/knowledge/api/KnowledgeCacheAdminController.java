package com.intelligenthealthcare.knowledge.api;

import com.intelligenthealthcare.knowledge.application.KnowledgeQueryApplicationService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/knowledge/cache")
@RequiredArgsConstructor
public class KnowledgeCacheAdminController {

    private final KnowledgeQueryApplicationService knowledgeQueryApplicationService;

    @PostMapping("/evict")
    public Map<String, Object> evict() {
        knowledgeQueryApplicationService.evictKnowledgeCaches();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "knowledge 缓存已清理");
        return result;
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        long start = System.currentTimeMillis();
        knowledgeQueryApplicationService.refreshHotCaches();
        long cost = System.currentTimeMillis() - start;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "knowledge 热点缓存已刷新");
        result.put("costMs", cost);
        return result;
    }
}
