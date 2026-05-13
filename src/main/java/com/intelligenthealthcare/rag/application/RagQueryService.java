package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.infrastructure.search.RagVectorSearchRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 查询向量：对问题做嵌入后执行近邻检索。
 * 当前口径为 MongoDB 向量检索；本实现为历史 PostgreSQL/pgvector 兼容链路。
 */
@Service
public class RagQueryService {

    private static final String HOT_CHUNK_CACHE = "knowledge:rag:hotChunk";

    private final EmbeddingModel embeddingModel;
    private final RagVectorSearchRepository vectorSearchRepository;
    private final RagProperties ragProperties;
    private final RedissonClient redissonClient;

    public RagQueryService(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            RagVectorSearchRepository vectorSearchRepository,
            RagProperties ragProperties,
            RedissonClient redissonClient) {
        this.embeddingModel = embeddingModel;
        this.vectorSearchRepository = vectorSearchRepository;
        this.ragProperties = ragProperties;
        this.redissonClient = redissonClient;
    }

    public List<RagSearchHitDto> search(String naturalLanguageQuery, int topK) {
        if (!StringUtils.hasText(naturalLanguageQuery)) {
            return List.of();
        }
        float[] q = embed(naturalLanguageQuery.trim());
        if (q.length != ragProperties.getEmbeddingDimensions()) {
            throw new IllegalStateException(
                    "查询嵌入维数 " + q.length + " 与 app.rag.embedding-dimensions=" + ragProperties.getEmbeddingDimensions() + " 不一致");
        }
        List<RagSearchHitDto> hotCacheHits = searchFromHotCache(q, topK);
        if (!hotCacheHits.isEmpty()) {
            return hotCacheHits;
        }
        return vectorSearchRepository.findNearestL2(q, topK);
    }

    private float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        return response.getResult().getOutput();
    }

    private List<RagSearchHitDto> searchFromHotCache(float[] queryEmbedding, int topK) {
        RMapCache<String, RagDocumentChunk> cache = redissonClient.getMapCache(HOT_CHUNK_CACHE);
        List<RagDocumentChunk> chunks = new ArrayList<>(cache.readAllValues());
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RagDocumentChunk chunk = chunks.get(i);
            float[] embedding = chunk.getEmbedding();
            if (embedding == null || embedding.length != queryEmbedding.length) {
                continue;
            }
            double distance = l2Distance(queryEmbedding, embedding);
            scored.add(new ScoredChunk(chunk, distance));
        }
        if (scored.isEmpty()) {
            return Collections.emptyList();
        }

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
