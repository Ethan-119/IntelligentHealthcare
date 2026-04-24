package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.infrastructure.search.RagVectorSearchRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 查询向量：对问题做嵌入后在 PostgreSQL 中做近邻检索。
 */
@Service
public class RagQueryService {

    private final EmbeddingModel embeddingModel;
    private final RagVectorSearchRepository vectorSearchRepository;
    private final RagProperties ragProperties;

    public RagQueryService(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            RagVectorSearchRepository vectorSearchRepository,
            RagProperties ragProperties) {
        this.embeddingModel = embeddingModel;
        this.vectorSearchRepository = vectorSearchRepository;
        this.ragProperties = ragProperties;
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
        return vectorSearchRepository.findNearestL2(q, topK);
    }

    private float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        return response.getResult().getOutput();
    }
}
