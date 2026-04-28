package com.intelligenthealthcare.rag.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.intelligenthealthcare.shared.infrastructure.mybatis.PgVectorFloatTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供 RAG 使用的文本块与向量。
 * 当前业务口径为 MongoDB 向量知识库。
 * 本实体保留用于兼容历史 PostgreSQL/pgvector 数据链路。
 */
@TableName("rag_document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("source_type")
    private RagSourceType sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("chunk_key")
    @Builder.Default
    private String chunkKey = "default";

    private String content;

    @TableField(value = "embedding", typeHandler = PgVectorFloatTypeHandler.class)
    private float[] embedding;
}
