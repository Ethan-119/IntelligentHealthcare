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

    /**
     * 滑动窗口切分大小（字符数），默认 500。
     */
    private int chunkWindowSize = 500;

    /**
     * 滑动窗口重叠大小（字符数），默认 100。
     */
    private int chunkOverlapSize = 100;

    /**
     * 重排序候选倍数，candidateK = topK * candidateMultiplier，默认 3。
     */
    private int candidateMultiplier = 3;

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(int embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public int getChunkWindowSize() {
        return chunkWindowSize;
    }

    public void setChunkWindowSize(int chunkWindowSize) {
        this.chunkWindowSize = chunkWindowSize;
    }

    public int getChunkOverlapSize() {
        return chunkOverlapSize;
    }

    public void setChunkOverlapSize(int chunkOverlapSize) {
        this.chunkOverlapSize = chunkOverlapSize;
    }

    public int getCandidateMultiplier() {
        return candidateMultiplier;
    }

    public void setCandidateMultiplier(int candidateMultiplier) {
        this.candidateMultiplier = candidateMultiplier;
    }
}
