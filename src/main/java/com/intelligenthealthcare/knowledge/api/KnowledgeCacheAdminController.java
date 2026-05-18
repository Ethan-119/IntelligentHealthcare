package com.intelligenthealthcare.knowledge.api;

import com.intelligenthealthcare.knowledge.api.dto.KnowledgeHotspotAddRequest;
import com.intelligenthealthcare.knowledge.application.KnowledgeQueryApplicationService;
import com.intelligenthealthcare.knowledge.application.KnowledgeQueryApplicationService.HotspotWarmupResult;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/hotspots/add")
    public Map<String, Object> addHotspots(@Valid @RequestBody KnowledgeHotspotAddRequest request) {
        HotspotWarmupResult stats = knowledgeQueryApplicationService.addHotspots(request.getScopes());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "热点缓存新增预热完成");
        result.put("scopes", stats.scopes());
        result.put("warmedEntries", stats.warmedEntries());
        return result;
    }
}
