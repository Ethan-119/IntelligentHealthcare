package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.repository.RagDocumentRepository;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 将可检索文本写入 MongoDB {@link RagDocumentChunk}，经 {@link EmbeddingModel} 生成向量。
 * 写入策略：先持久化向量文档，再同步写入 Redis 热缓存，供查询侧优先命中。
 */
@Service
public class RagIngestionService {

    private static final String HOT_CHUNK_CACHE = "knowledge:rag:hotChunk";
    private static final long HOT_CHUNK_TTL_MINUTES = 60L;

    private final EmbeddingModel embeddingModel;
    private final RagDocumentRepository ragDocumentRepository;
    private final RagProperties ragProperties;
    private final RedissonClient redissonClient;

    // Lombok @RequiredArgsConstructor 不适用：embeddingModel 需要 @Qualifier 限定
    public RagIngestionService(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            RagDocumentRepository ragDocumentRepository,
            RagProperties ragProperties,
            RedissonClient redissonClient) {
        this.embeddingModel = embeddingModel;
        this.ragDocumentRepository = ragDocumentRepository;
        this.ragProperties = ragProperties;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public String upsert(RagIngestCommand command) {
        String chunkKey = StringUtils.hasText(command.chunkKey()) ? command.chunkKey().trim() : "default";
        float[] vector = embed(command.content());
        if (vector.length != ragProperties.getEmbeddingDimensions()) {
            throw new IllegalStateException(
                    "嵌入维数 " + vector.length + " 与 app.rag.embedding-dimensions="
                            + ragProperties.getEmbeddingDimensions() + " 不一致");
        }
        Optional<RagDocumentChunk> existing = ragDocumentRepository
                .findBySourceTypeAndSourceIdAndChunkKey(
                        command.sourceType(), command.sourceId().trim(), chunkKey);
        RagDocumentChunk entity;
        if (existing.isPresent()) {
            // 幂等更新：同 sourceType/sourceId/chunkKey 视为同一文本块，更新而不是新增。
            entity = existing.get();
            entity.setContent(command.content());
            entity.setEmbedding(vector);
            ragDocumentRepository.update(entity);
            writeHotChunkCache(entity);
            return entity.getId();
        }
        entity = RagDocumentChunk.builder()
                .sourceType(command.sourceType())
                .sourceId(command.sourceId().trim())
                .chunkKey(chunkKey)
                .content(command.content())
                .embedding(vector)
                .build();
        ragDocumentRepository.insert(entity);
        writeHotChunkCache(entity);
        return entity.getId();
    }

    private float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
        return response.getResult().getOutput();
    }

    private void writeHotChunkCache(RagDocumentChunk chunk) {
        String cacheKey = buildChunkCacheKey(chunk);
        RMapCache<String, RagDocumentChunk> cache = redissonClient.getMapCache(HOT_CHUNK_CACHE);
        // 热缓存与查询侧 TTL 对齐，保证近期高频文档优先命中。
        cache.put(cacheKey, chunk, HOT_CHUNK_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private String buildChunkCacheKey(RagDocumentChunk chunk) {
        String sourceTypeCode = "";
        if (chunk.getSourceType() != null) {
            sourceTypeCode = chunk.getSourceType().getCode();
        }
        return sourceTypeCode + ":" + chunk.getSourceId() + ":" + chunk.getChunkKey();
    }
}
