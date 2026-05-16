package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.repository.RagDocumentRepository;
import com.intelligenthealthcare.rag.infrastructure.embedding.EmbeddingUtil;
import java.util.List;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 将可检索文本写入 MongoDB {@link RagDocumentChunk}，经 {@link EmbeddingUtil} 生成向量。
 * 写入策略：先持久化向量文档，再同步写入 Redis 热缓存，供查询侧优先命中。
 */
@Service
public class RagIngestionService {

    private static final String HOT_CHUNK_CACHE = "knowledge:rag:hotChunk";
    private static final long HOT_CHUNK_TTL_MINUTES = 60L;

    private final EmbeddingUtil embeddingUtil;
    private final RagDocumentRepository ragDocumentRepository;
    private final RagProperties ragProperties;
    private final RedissonClient redissonClient;

    public RagIngestionService(
            EmbeddingUtil embeddingUtil,
            RagDocumentRepository ragDocumentRepository,
            RagProperties ragProperties,
            RedissonClient redissonClient) {
        this.embeddingUtil = embeddingUtil;
        this.ragDocumentRepository = ragDocumentRepository;
        this.ragProperties = ragProperties;
        this.redissonClient = redissonClient;
    }

    // 不使用 @Transactional：Spring 事务管理器仅覆盖 JDBC，MongoDB 操作不参与。
    // 若 Redis 缓存写入失败而 MongoDB 已写入，不一致需由上层补偿逻辑处理。
    public String upsert(RagIngestCommand command) {
        String chunkKey = StringUtils.hasText(command.chunkKey()) ? command.chunkKey().trim() : "default";
        float[] vector = embeddingUtil.embed(command.content());
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
