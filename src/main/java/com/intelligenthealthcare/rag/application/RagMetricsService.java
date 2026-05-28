package com.intelligenthealthcare.rag.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG 查询耗时统计，记录一次检索中各个阶段的耗时与关键指标。
 * 使用方式：在 {@link RagQueryService} 中创建 timer，逐阶段填充数据，最后调用 {@link RagTimer#done()} 输出日志。
 */
@Component
public class RagMetricsService {

    private static final Logger log = LoggerFactory.getLogger(RagMetricsService.class);

    public RagTimer start(String query, int topK, int candidateK) {
        return new RagTimer(query, topK, candidateK);
    }

    public static class RagTimer {
        private final String query;
        private final int topK;
        private final int candidateK;
        private final long startNanos;

        // 各阶段耗时(ms)
        long embedMs;
        int embedDim;
        long searchMs;
        int searchResultCount;
        boolean cacheHit;
        long rerankMs;
        int rerankInput;
        int rerankOutput;
        boolean rerankFallback;

        RagTimer(String query, int topK, int candidateK) {
            this.query = query;
            this.topK = topK;
            this.candidateK = candidateK;
            this.startNanos = System.nanoTime();
        }

        public RagTimer embed(long ms, int dim) {
            this.embedMs = ms;
            this.embedDim = dim;
            return this;
        }

        public RagTimer search(long ms, int count, boolean cacheHit) {
            this.searchMs = ms;
            this.searchResultCount = count;
            this.cacheHit = cacheHit;
            return this;
        }

        public RagTimer rerank(long ms, int input, int output, boolean fallback) {
            this.rerankMs = ms;
            this.rerankInput = input;
            this.rerankOutput = output;
            this.rerankFallback = fallback;
            return this;
        }

        public void done() {
            long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
            String q = query.length() > 50 ? query.substring(0, 50) + "..." : query;
            log.info("[RAG] 总{}ms | embed:{}ms/{}维 | {}检索:{}ms/{}条 | rerank:{}ms/{}→{}{} | topK={} candidateK={} | query=\"{}\"",
                    totalMs,
                    embedMs, embedDim,
                    cacheHit ? "热缓存" : "向量", searchMs, searchResultCount,
                    rerankMs, rerankInput, rerankOutput, rerankFallback ? "(降级)" : "",
                    topK, candidateK,
                    q);
        }
    }
}
