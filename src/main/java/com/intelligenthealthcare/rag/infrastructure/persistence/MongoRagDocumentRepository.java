package com.intelligenthealthcare.rag.infrastructure.persistence;

import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import com.intelligenthealthcare.rag.domain.repository.RagDocumentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MongoRagDocumentRepository implements RagDocumentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<RagDocumentChunk> findBySourceTypeAndSourceIdAndChunkKey(
            RagSourceType sourceType, String sourceId, String chunkKey) {
        Query query = new Query();
        query.addCriteria(Criteria.where("sourceType").is(sourceType)
                .and("sourceId").is(sourceId)
                .and("chunkKey").is(chunkKey));
        return Optional.ofNullable(mongoTemplate.findOne(query, RagDocumentChunk.class));
    }

    @Override
    public void insert(RagDocumentChunk entity) {
        mongoTemplate.insert(entity);
    }

    @Override
    public void update(RagDocumentChunk entity) {
        mongoTemplate.save(entity);
    }
}
