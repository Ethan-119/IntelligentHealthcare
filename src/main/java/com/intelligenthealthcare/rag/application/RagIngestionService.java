package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.infrastructure.persistence.RagDocumentChunkMapper;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;

/**
 * 将可检索文本写入 {@link RagDocumentChunk}，经 {@link EmbeddingModel} 生成向量。
 */
@Service
public class RagIngestionService {

    private final EmbeddingModel embeddingModel;
    private final RagDocumentChunkMapper ragDocumentChunkMapper;
    private final RagProperties ragProperties;

    public RagIngestionService(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            RagDocumentChunkMapper ragDocumentChunkMapper,
            RagProperties ragProperties) {
        this.embeddingModel = embeddingModel;
        this.ragDocumentChunkMapper = ragDocumentChunkMapper;
        this.ragProperties = ragProperties;
    }

    @Transactional
    public long upsert(RagIngestCommand command) {
        String chunkKey =
                StringUtils.hasText(command.chunkKey()) ? command.chunkKey().trim() : "default";
        float[] vector = embed(command.content());
        if (vector.length != ragProperties.getEmbeddingDimensions()) {
            throw new IllegalStateException(
                    "嵌入维数 " + vector.length + " 与 app.rag.embedding-dimensions=" + ragProperties.getEmbeddingDimensions() + " 不一致");
        }
        var existing =
                ragDocumentChunkMapper.findBySourceTypeAndSourceIdAndChunkKey(
                        command.sourceType(), command.sourceId().trim(), chunkKey);
        RagDocumentChunk entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setContent(command.content());
            entity.setEmbedding(vector);
            ragDocumentChunkMapper.updateById(entity);
            return entity.getId();
        }
        entity =
                RagDocumentChunk.builder()
                        .sourceType(command.sourceType())
                        .sourceId(command.sourceId().trim())
                        .chunkKey(chunkKey)
                        .content(command.content())
                        .embedding(vector)
                        .build();
        ragDocumentChunkMapper.insert(entity);
        return entity.getId();
    }

    private float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        return response.getResult().getOutput();
    }
}
