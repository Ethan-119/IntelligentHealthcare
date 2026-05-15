package com.intelligenthealthcare.rag.domain.repository;

import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import java.util.Optional;

public interface RagDocumentRepository {
    Optional<RagDocumentChunk> findBySourceTypeAndSourceIdAndChunkKey(
            RagSourceType sourceType, String sourceId, String chunkKey);
    void insert(RagDocumentChunk entity);
    void update(RagDocumentChunk entity);
}
