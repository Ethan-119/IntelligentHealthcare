package com.intelligenthealthcare.rag.application.dto;

/**
 * 向量近邻结果（L2 距离，越小越近）。
 */
public record RagSearchHitDto(
        long id,
        String sourceType,
        String sourceId,
        String chunkKey,
        String content,
        double distance) {}
