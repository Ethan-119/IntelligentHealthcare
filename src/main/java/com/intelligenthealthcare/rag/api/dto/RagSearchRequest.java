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

    /**
     * 两阶段检索的候选数量：0 表示使用默认倍数（topK * 3），
     * 显式设置且大于 topK 时触发重排序：先召回 candidateK 条，再重排序取 topK 条。
     */
    @Min(0)
    @Max(500)
    private int candidateK = 0;
}
