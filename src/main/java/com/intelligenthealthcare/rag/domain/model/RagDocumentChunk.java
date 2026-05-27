package com.intelligenthealthcare.rag.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * RAG 文本块与向量，存储在 MongoDB 中。
 */
@Document(collection = "rag_document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentChunk {

    @Id
    private String id;

    @Field("source_type")
    @Indexed
    private RagSourceType sourceType;

    @Field("source_id")
    @Indexed
    private String sourceId;

    @Field("chunk_key")
    @Builder.Default
    private String chunkKey = "default";

    private String content;

    @Field("embedding")
    private float[] embedding;

    /**
     * 是否启用：false 时向量检索和热缓存均跳过该块，相当于"下架"。
     * 管理员可随时在 MongoDB 中修改此字段。
     */
    @Field("active")
    @Builder.Default
    private boolean active = true;
}
