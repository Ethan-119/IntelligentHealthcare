package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.config.RagProperties;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import com.intelligenthealthcare.rag.domain.repository.RagDocumentRepository;
import com.intelligenthealthcare.rag.infrastructure.chunking.SlidingWindowChunker;
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
    private final SlidingWindowChunker chunker;

    public RagIngestionService(
            EmbeddingUtil embeddingUtil,
            RagDocumentRepository ragDocumentRepository,
            RagProperties ragProperties,
            RedissonClient redissonClient) {
        this.embeddingUtil = embeddingUtil;
        this.ragDocumentRepository = ragDocumentRepository;
        this.ragProperties = ragProperties;
        this.redissonClient = redissonClient;
        this.chunker = new SlidingWindowChunker(
                ragProperties.getChunkWindowSize(),
                ragProperties.getChunkOverlapSize());
    }

    // 不使用 @Transactional：Spring 事务管理器仅覆盖 JDBC，MongoDB 操作不参与。
    // 若 Redis 缓存写入失败而 MongoDB 已写入，不一致需由上层补偿逻辑处理。
    public String upsert(RagIngestCommand command) {
        return upsert(command, true);
    }

    /**
     * @param command   摄入命令
     * @param writeCache 是否写入 Redis 热缓存。批量导入时传 false，避免冷数据占满缓存。
     */
    public String upsert(RagIngestCommand command, boolean writeCache) {
        String chunkKey = StringUtils.hasText(command.chunkKey()) ? command.chunkKey().trim() : "default";
        float[] vector = embeddingUtil.embed(command.content());
        if (vector.length != ragProperties.getEmbeddingDimensions()) {
            throw new IllegalStateException(
                    "嵌入维数 " + vector.length + " 与 app.rag.embedding-dimensions="
                            + ragProperties.getEmbeddingDimensions() + " 不一致 ");
        }
        Optional<RagDocumentChunk> existing = ragDocumentRepository
                .findBySourceTypeAndSourceIdAndChunkKey(
                        command.sourceType(), command.sourceId().trim(), chunkKey);
        RagDocumentChunk entity;
        //新数据来了 ，存在即更新，不存在则新增
        if (existing.isPresent()) {
            // 幂等更新：同 sourceType/sourceId/chunkKey 视为同一文本块，更新而不是新增。
            entity = existing.get();//从 Optional 包装器里，把真实的数据库记录（对象）给”拿”出来
            entity.setContent(command.content());
            entity.setEmbedding(vector);
            ragDocumentRepository.update(entity);
            if (writeCache) {
                writeHotChunkCache(entity);
            }
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
        if (writeCache) {
            writeHotChunkCache(entity);
        }
        return entity.getId();
    }

    /**
     * 对一篇文档执行全流程摄入：删除旧块 → 滑动窗口切分 → 逐块调用 {@link #upsert(RagIngestCommand)}。
     *
     * @param documentName 文档名，作为 sourceId
     * @param text         文档全文
     * @return 切分出的块数量
     */
    public int ingestDocument(String documentName, String text) {
        RagSourceType sourceType = RagSourceType.DOCUMENT;
        String sourceId = documentName;

        // 删除同文档的旧块，保证重传幂等
        ragDocumentRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);

        // 滑动窗口切分
        List<SlidingWindowChunker.ChunkPair> chunks = chunker.chunk(text, sourceId);
        if (chunks.isEmpty()) {
            return 0;
        }

        // 逐块摄入（不写热缓存：批量文档数据量大，冷数据占满缓存反而拖慢 L2 遍历）
        for (int i = 0; i < chunks.size(); i++) {
            SlidingWindowChunker.ChunkPair pair = chunks.get(i);
            RagIngestCommand ingestCmd = new RagIngestCommand(
                    sourceType,
                    sourceId,
                    pair.getChunkKey(),
                    pair.getContent());
            upsert(ingestCmd, false);
        }
        return chunks.size();
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
