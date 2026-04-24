package com.intelligenthealthcare.rag.application;

import com.intelligenthealthcare.rag.domain.model.RagSourceType;

/**
 * 建库/更新可检索语料的用例入参，与 Web DTO 解耦。
 */
public record RagIngestCommand(
        RagSourceType sourceType, String sourceId, String chunkKey, String content) {}
