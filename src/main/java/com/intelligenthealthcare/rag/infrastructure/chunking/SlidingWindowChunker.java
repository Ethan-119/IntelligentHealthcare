package com.intelligenthealthcare.rag.infrastructure.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 滑动窗口文本切分器：按固定窗口大小和重叠量将长文本切分为有重叠的片段，
 * 保证上下文连贯性，便于后续向量检索。
 */
public class SlidingWindowChunker {

    private final int windowSize;
    private final int overlapSize;

    public SlidingWindowChunker(int windowSize, int overlapSize) {
        this.windowSize = windowSize;
        this.overlapSize = overlapSize;
    }

    /**
     * 将文本切分为有重叠的片段列表。
     *
     * @param text     原始文本
     * @param sourceId 来源标识，用于生成 chunkKey
     * @return 切分后的 (chunkKey, content) 列表，文本为空时返回空列表
     */
    public List<ChunkPair> chunk(String text, String sourceId) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = text.trim();
        if (trimmed.length() <= windowSize) {
            String key = buildChunkKey(sourceId, 0);
            return Collections.singletonList(new ChunkPair(key, trimmed));
        }

        int step = windowSize - overlapSize;
        if (step <= 0) {
            step = windowSize;
        }

        List<ChunkPair> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;
        while (start < trimmed.length()) {
            int end = Math.min(start + windowSize, trimmed.length());
            String content = trimmed.substring(start, end);
            String key = buildChunkKey(sourceId, index);
            chunks.add(new ChunkPair(key, content));
            index++;
            start += step;
        }
        return chunks;
    }

    private static String buildChunkKey(String sourceId, int index) {
        String padded = String.format("%04d", index);
        return sourceId + "_chunk_" + padded;
    }

    /**
     * 切分结果：包含块标识和块内容。
     */
    public static class ChunkPair {
        private final String chunkKey;
        private final String content;

        public ChunkPair(String chunkKey, String content) {
            this.chunkKey = chunkKey;
            this.content = content;
        }

        public String getChunkKey() {
            return chunkKey;
        }

        public String getContent() {
            return content;
        }
    }
}
