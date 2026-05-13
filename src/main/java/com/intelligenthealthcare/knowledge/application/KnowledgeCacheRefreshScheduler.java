package com.intelligenthealthcare.knowledge.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeCacheRefreshScheduler {

    private final KnowledgeQueryApplicationService knowledgeQueryApplicationService;

    /**
     * 在离峰时段（默认 00:00-03:00）定时刷新知识库热点缓存。
     */
    @Scheduled(
            cron = "${app.cache.knowledge.offpeak-refresh-cron:0 0/30 0-2 * * *}",
            zone = "${app.cache.knowledge.offpeak-refresh-zone:Asia/Shanghai}")
    public void refreshHotCaches() {
        long start = System.currentTimeMillis();
        knowledgeQueryApplicationService.refreshHotCaches();
        long cost = System.currentTimeMillis() - start;
        log.info("knowledge 离峰热点缓存刷新完成，耗时 {} ms", cost);
    }
}
