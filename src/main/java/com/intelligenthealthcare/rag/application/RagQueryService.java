package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.infrastructure.embedding.EmbeddingUtil;
import com.intelligenthealthcare.rag.infrastructure.rerank.RerankService;
import com.intelligenthealthcare.rag.infrastructure.search.RagVectorSearchRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 查询向量：对问题做嵌入后执行近邻检索。
 * 当前口径为 MongoDB 向量检索；本实现为历史 PostgreSQL/pgvector 兼容链路。
 * <p>
 * 查询策略：优先读 Redis 热缓存（低延迟），未命中再回落到向量库检索。
 */
@Service
public class RagQueryService {

    private static final String HOT_CHUNK_CACHE = "knowledge:rag:hotChunk";

    private final EmbeddingUtil embeddingUtil;
    private final RagVectorSearchRepository vectorSearchRepository;
    private final RagProperties ragProperties;
    private final RedissonClient redissonClient;
    private final RerankService rerankService;

    public RagQueryService(
            EmbeddingUtil embeddingUtil,
            RagVectorSearchRepository vectorSearchRepository,
            RagProperties ragProperties,
            RedissonClient redissonClient,
            RerankService rerankService) {
        this.embeddingUtil = embeddingUtil;
        this.vectorSearchRepository = vectorSearchRepository;
        this.ragProperties = ragProperties;
        this.redissonClient = redissonClient;
        this.rerankService = rerankService;
    }

    public List<RagSearchHitDto> search(String naturalLanguageQuery, int topK) {
        return search(naturalLanguageQuery, topK, 0);
    }

    /**
     * 两阶段检索：先向量召回 candidateK 条，再经重排序模型精排返回 topK 条。
     *
     * @param naturalLanguageQuery 查询文本
     * @param topK                 最终返回条数
     * @param candidateK           候选条数，0 或 <= topK 时不重排序
     */
    public List<RagSearchHitDto> search(String naturalLanguageQuery, int topK, int candidateK) {
        if (!StringUtils.hasText(naturalLanguageQuery)) {
            return List.of();
        }
        float[] q = embeddingUtil.embed(naturalLanguageQuery.trim());
        if (q.length != ragProperties.getEmbeddingDimensions()) {
            throw new IllegalStateException(
                    "查询嵌入维数 " + q.length + " 与 app.rag.embedding-dimensions=" + ragProperties.getEmbeddingDimensions() + " 不一致");
        }

        // 计算实际召回数量
        int effectiveCandidateK = candidateK > topK ? candidateK : topK; //真实返回个数，只有当超过了上限才返回上限

        // 尝试热缓存
        List<RagSearchHitDto> hotCacheHits = searchFromHotCache(q, effectiveCandidateK);
        if (!hotCacheHits.isEmpty()) {
            return rerankIfNeeded(naturalLanguageQuery, hotCacheHits, topK, effectiveCandidateK);
        }

        // 向量库检索
        List<RagSearchHitDto> candidates = vectorSearchRepository.findNearestL2(q, effectiveCandidateK);
        return rerankIfNeeded(naturalLanguageQuery, candidates, topK, effectiveCandidateK);
    }

    /**
     * 如果候选数大于最终返回数，调用重排序模型精排；否则直接截断返回。
     */
    private List<RagSearchHitDto> rerankIfNeeded(String query, List<RagSearchHitDto> candidates, int topK, int effectiveCandidateK) {
        if (candidates.size() <= topK || effectiveCandidateK <= topK) {
            // 候选不足，直接截断
            if (candidates.size() <= topK) {
                return candidates;
            }
            return candidates.subList(0, topK);
        }

        // 提取候选文本
        List<String> candidateTexts = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            candidateTexts.add(candidates.get(i).content());
        }

        // 调用重排序
        List<Integer> rerankedIndices = rerankService.rerank(query, candidateTexts, topK);
        if (rerankedIndices == null || rerankedIndices.isEmpty()) {
            // 重排序失败，退回到 L2 排序的前 topK 条
            return candidates.subList(0, topK);
        }

        // 按重排序结果组装
        List<RagSearchHitDto> result = new ArrayList<>();
        for (int i = 0; i < rerankedIndices.size(); i++) {
            int idx = rerankedIndices.get(i);
            if (idx >= 0 && idx < candidates.size()) {
                result.add(candidates.get(idx));
            }
        }
        return result;
    }

    private List<RagSearchHitDto> searchFromHotCache(float[] queryEmbedding, int topK) {
        RMapCache<String, RagDocumentChunk> cache = redissonClient.getMapCache(HOT_CHUNK_CACHE);
        List<RagDocumentChunk> chunks = new ArrayList<>(cache.readAllValues());
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RagDocumentChunk chunk = chunks.get(i);//获取对象
            // 跳过已下架（active=false）的块
            if (!chunk.isActive()) {
                continue;
            }
            float[] embedding = chunk.getEmbedding();//获取该对象的向量信息
            if (embedding == null || embedding.length != queryEmbedding.length) {
                // 向量维度不一致的脏数据直接跳过，避免污染排序结果。
                continue;
            }
            double distance = l2Distance(queryEmbedding, embedding);
            scored.add(new ScoredChunk(chunk, distance));
        }
        if (scored.isEmpty()) {
            return Collections.emptyList();
        }

        //初次筛选
        scored.sort(new Comparator<ScoredChunk>() {
            @Override
            public int compare(ScoredChunk a, ScoredChunk b) {
                return Double.compare(a.distance, b.distance);
            }
        });

        int limit = Math.min(topK, scored.size());
        List<RagSearchHitDto> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            ScoredChunk item = scored.get(i);
            RagDocumentChunk chunk = item.chunk;
            String sourceType = chunk.getSourceType() == null ? null : chunk.getSourceType().getCode();
            result.add(new RagSearchHitDto(
                    chunk.getId(),
                    sourceType,
                    chunk.getSourceId(),
                    chunk.getChunkKey(),
                    chunk.getContent(),
                    item.distance));
        }
        return result;
    }

    private static double l2Distance(float[] a, float[] b) {
        // 与向量库检索口径保持一致：使用 L2 距离，值越小语义越接近。
        double sum = 0.0D;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static class ScoredChunk {
        private final RagDocumentChunk chunk;
        private final double distance;

        private ScoredChunk(RagDocumentChunk chunk, double distance) {
            this.chunk = chunk;
            this.distance = distance;
        }
    }
}
