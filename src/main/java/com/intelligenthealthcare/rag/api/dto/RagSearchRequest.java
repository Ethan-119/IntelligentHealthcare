package com.intelligenthealthcare.rag.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagSearchRequest {

    @NotBlank
    private String query;

    @Min(1)
    @Max(100)
    private int topK = 5;
}
