package com.intelligenthealthcare.rag.infrastructure.search;

import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import com.intelligenthealthcare.rag.config.RagProperties;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL pgvector 历史兼容实现：使用 L2 距离算子；参数以文本字面量经 {@code ::vector} 绑定。
 * 当前业务口径为 MongoDB 向量检索。
 */
@Repository
public class RagVectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String searchSql;

    public RagVectorSearchRepository(JdbcTemplate jdbcTemplate, RagProperties ragProperties) {
        this.jdbcTemplate = jdbcTemplate;
        int dim = ragProperties.getEmbeddingDimensions();
        this.searchSql =
                """
                SELECT id, source_type, source_id, chunk_key, content, (embedding <-> cast(? as vector(%d))) AS dist
                FROM rag_document_chunks
                ORDER BY dist ASC
                LIMIT ?
                """
                        .formatted(dim);
    }

    public List<RagSearchHitDto> findNearestL2(float[] queryEmbedding, int topK) {
        String literal = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(
                searchSql,
                (rs, row) ->
                        new RagSearchHitDto(
                                rs.getLong("id"),
                                rs.getString("source_type"),
                                rs.getString("source_id"),
                                rs.getString("chunk_key"),
                                rs.getString("content"),
                                rs.getDouble("dist")),
                literal,
                topK);
    }

    private static String toVectorLiteral(float[] v) {
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("queryEmbedding 不能为空");
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
