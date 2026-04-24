package com.intelligenthealthcare.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 向量列维度需与通义 text-embedding-v3 的 {@code dimensions} 配置一致。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /**
     * 与表列 {@code vector(N)} 及百炼 {@code text-embedding-v3} 的 output 维数一致。
     */
    private int embeddingDimensions = 1024;

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(int embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }
}
