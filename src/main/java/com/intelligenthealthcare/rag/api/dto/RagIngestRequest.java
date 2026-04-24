package com.intelligenthealthcare.rag.api.dto;

import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagIngestRequest {

    @NotNull
    private RagSourceType sourceType;

    @NotBlank
    private String sourceId;

    /** 同一 source 下可有多块，如分片键；可省略，服务端按 default 处理。 */
    private String chunkKey;

    @NotBlank
    private String content;
}
