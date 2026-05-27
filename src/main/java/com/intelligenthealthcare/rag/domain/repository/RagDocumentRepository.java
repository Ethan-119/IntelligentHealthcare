package com.intelligenthealthcare.rag.domain.repository;

import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import java.util.Optional;

public interface RagDocumentRepository {
    Optional<RagDocumentChunk> findBySourceTypeAndSourceIdAndChunkKey(
            RagSourceType sourceType, String sourceId, String chunkKey);
    void insert(RagDocumentChunk entity);
    void update(RagDocumentChunk entity);

    void deleteBySourceTypeAndSourceId(RagSourceType sourceType, String sourceId);

    java.util.List<String> findDistinctSourceIdsBySourceType(RagSourceType sourceType);

    // 批量设置一个文档所有块的启用/停用状态
    void setActiveBySourceTypeAndSourceId(RagSourceType sourceType, String sourceId, boolean active);
}
