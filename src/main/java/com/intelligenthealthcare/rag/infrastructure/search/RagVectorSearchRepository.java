package com.intelligenthealthcare.rag.infrastructure.search;

import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MongoDB 向量检索：对候选文档计算 L2 距离后排序返回 topK。
 * 生产环境大规模数据建议迁移至 MongoDB Atlas Vector Search。
 */
@Repository
public class RagVectorSearchRepository {

    private final MongoTemplate mongoTemplate;

    public RagVectorSearchRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<RagSearchHitDto> findNearestL2(float[] queryEmbedding, int topK) {
        // 检索 active!=false 的块：active=true 或缺失 active 字段（旧数据）均参与检索，
        // 只有管理员显式下架（active=false）才跳过。
        Query query = new Query();
        query.addCriteria(Criteria.where("active").ne(false));
        List<RagDocumentChunk> all = mongoTemplate.find(query, RagDocumentChunk.class);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scored = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            RagDocumentChunk chunk = all.get(i);
            float[] emb = chunk.getEmbedding();
            if (emb != null && emb.length == queryEmbedding.length) {
                double dist = l2Distance(queryEmbedding, emb);
                scored.add(new ScoredChunk(chunk, dist));
            }
        }

        Collections.sort(scored, new Comparator<ScoredChunk>() {
            @Override
            public int compare(ScoredChunk a, ScoredChunk b) {
                return Double.compare(a.dist, b.dist);
            }
        });

        int limit = Math.min(topK, scored.size());
        List<RagSearchHitDto> results = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            ScoredChunk s = scored.get(i);
            RagDocumentChunk chunk = s.chunk;
            String sourceType = chunk.getSourceType() != null ? chunk.getSourceType().getCode() : null;
            RagSearchHitDto hit = new RagSearchHitDto(
                    chunk.getId(),
                    sourceType,
                    chunk.getSourceId(),
                    chunk.getChunkKey(),
                    chunk.getContent(),
                    s.dist);
            results.add(hit);
        }
        return results;
    }

    private static double l2Distance(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static class ScoredChunk {
        final RagDocumentChunk chunk;
        final double dist;

        ScoredChunk(RagDocumentChunk chunk, double dist) {
            this.chunk = chunk;
            this.dist = dist;
        }
    }
}
