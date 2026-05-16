package com.intelligenthealthcare.rag.infrastructure.embedding;

import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 文本向量化工具，统一封装 EmbeddingModel 调用，消除 {@code RagQueryService} 与
 * {@code RagIngestionService} 中重复的 {@code embed()} 逻辑。
 */
@Component
public class EmbeddingUtil {

    private final EmbeddingModel embeddingModel;

    public EmbeddingUtil(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        return response.getResult().getOutput();
    }
}
