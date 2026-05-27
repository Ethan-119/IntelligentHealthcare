package com.intelligenthealthcare.rag.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentUploadResponse {
    private String documentName;
    private int chunkCount;
    private String message;
}
