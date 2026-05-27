package com.intelligenthealthcare.rag.infrastructure.rerank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 调用 DashScope gte-rerank 模型对候选文档进行语义重排序。
 * 失败时降级返回 null，由上游退回到原 L2 距离排序。
 */
@Component
public class RerankService {

    private static final Logger log = LoggerFactory.getLogger(RerankService.class);
    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank";
    private static final String MODEL = "gte-rerank";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public RerankService(@Value("${spring.ai.openai.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 对候选文档进行重排序。
     *
     * @param query      原始查询文本
     * @param documents  候选文档文本列表
     * @param topN       返回前 N 条
     * @return 按相关性降序排列的候选文档索引列表；失败时返回 null
     */
    public List<Integer> rerank(String query, List<String> documents, int topN) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = buildRequestBody(query, documents, topN);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RERANK_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("重排序 API 返回非 2xx: {}", response.getStatusCode());
                return null;
            }

            return parseResponse(response.getBody(), topN);
        } catch (Exception e) {
            log.warn("重排序调用失败，降级为原 L2 排序: {}", e.getMessage());
            return null;
        }
    }

    private String buildRequestBody(String query, List<String> documents, int topN) throws Exception {
        // 手动构造 JSON，避免引入额外依赖
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(MODEL).append("\",");
        sb.append("\"input\":{");
        sb.append("\"query\":").append(jsonString(query)).append(",");
        sb.append("\"documents\":[");
        for (int i = 0; i < documents.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonString(documents.get(i)));
        }
        sb.append("]},");
        sb.append("\"parameters\":{\"top_n\":").append(topN).append("}}");
        return sb.toString();
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private List<Integer> parseResponse(String responseBody, int topN) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("output").path("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            log.warn("重排序返回结果为空");
            return Collections.emptyList();
        }

        List<IndexScore> scored = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JsonNode item = results.get(i);
            int index = item.path("index").asInt();
            double score = item.path("relevance_score").asDouble();
            scored.add(new IndexScore(index, score));
        }

        // 按分数降序排序
        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            indices.add(scored.get(i).index);
        }
        return indices;
    }

    private static class IndexScore {
        final int index;
        final double score;

        IndexScore(int index, double score) {
            this.index = index;
            this.score = score;
        }
    }
}
