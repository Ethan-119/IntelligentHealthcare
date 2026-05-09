package com.intelligenthealthcare.rag.infrastructure.persistence;

import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB 实现的 RAG 文档块持久化，替代原 MyBatis RagDocumentChunkMapper。
 */
@Repository
public class MongoRagDocumentRepository {

    private final MongoTemplate mongoTemplate;

    public MongoRagDocumentRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<RagDocumentChunk> findBySourceTypeAndSourceIdAndChunkKey(
            RagSourceType sourceType, String sourceId, String chunkKey) {
        Query query = new Query();
        query.addCriteria(Criteria.where("sourceType").is(sourceType)
                .and("sourceId").is(sourceId)
                .and("chunkKey").is(chunkKey));
        return Optional.ofNullable(mongoTemplate.findOne(query, RagDocumentChunk.class));
    }

    public void insert(RagDocumentChunk entity) {
        mongoTemplate.insert(entity);
    }

    public void update(RagDocumentChunk entity) {
        mongoTemplate.save(entity);
    }
}
