package com.intelligenthealthcare.rag.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {

    private List<RagSearchItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagSearchItem {
        private long id;
        private String sourceType;
        private String sourceId;
        private String chunkKey;
        private String content;
        /** 与近邻算子 {@code <->} 返回一致：L2 距离，数值越小越相似。 */
        private double distance;
    }
}
