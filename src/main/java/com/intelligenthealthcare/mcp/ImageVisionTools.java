package com.intelligenthealthcare.mcp;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * MCP 图片视觉分析工具，通过 DashScope 多模态模型分析医学图片。
 * 方法上标注 {@link Tool} 后，Spring AI MCP Server 将自动暴露为可发现的工具。
 * <p>
 * 设计约束：
 * 1) 入参统一接收 Base64 / data URL，避免前端与后端在文件存储协议上耦合；
 * 2) 单次分析数量受 {@code app.vision.max-images} 控制，防止大批量图片拖慢请求；
 * 3) 单图失败不影响整批，返回部分可用结果，提升导诊可用性。
 */
@Slf4j
@Component
public class ImageVisionTools {

    private static final Pattern DATA_URL_PATTERN =
            Pattern.compile("^data:(image/[a-zA-Z+]+);base64,(.+)$", Pattern.DOTALL);

    private final ChatClient visionChatClient;
    private final boolean enabled;
    private final int maxImages;

    public ImageVisionTools(
            ChatClient.Builder chatClientBuilder,
            @Value("${app.vision.enabled:true}") boolean enabled,
            @Value("${app.vision.model:qwen-vl-plus}") String visionModel,
            @Value("${app.vision.max-images:5}") int maxImages) {
        this.enabled = enabled;
        this.maxImages = maxImages;
        this.visionChatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(visionModel)
                        .maxTokens(1024)
                        .build())
                .build();
    }

    /**
     * MCP 工具：分析医学图片。当用户上传症状照片、检查报告截图等图片时，
     * AI Agent 可通过 MCP 协议发现并调用此工具获取图片的结构化分析结果。
     *
     * @param base64Images  Base64 编码的图片列表（支持 data: URL 前缀或纯 base64）
     * @param medicalContext 当前医学上下文（症状描述、患者信息等）
     * @return 图片分析结果文本
     */
    @Tool(description = "分析用户上传的医学图片（症状照片、检查报告等），提取关键视觉信息")
    public String analyzeMedicalImages(
            @ToolParam(description = "Base64 编码的图片数据列表") List<String> base64Images,
            @ToolParam(description = "患者症状描述、个人信息等上下文") String medicalContext) {
        return analyzeMedicalImages(base64Images, medicalContext, null);
    }

    public String analyzeMedicalImages(
            List<String> base64Images,
            String medicalContext,
            Integer maxAnalyzeCount) {
        if (!enabled) {
            return "视觉分析服务未启用。";
        }
        if (base64Images == null || base64Images.isEmpty()) {
            return "未提供图片数据。";
        }

        List<byte[]> decodedImages = new ArrayList<>();
        for (String raw : base64Images) {
            DecodedImage decoded = decodeImage(raw);
            if (decoded != null) {
                decodedImages.add(decoded.data);
            }
        }
        if (decodedImages.isEmpty()) {
            return "无法解析任何图片数据。";
        }

        int effectiveLimit = maxImages;
        if (maxAnalyzeCount != null && maxAnalyzeCount > 0) {
            effectiveLimit = Math.min(maxAnalyzeCount, decodedImages.size());
        }
        int analyzeCount = Math.min(decodedImages.size(), effectiveLimit);
        List<String> results = new ArrayList<>();
        for (int i = 0; i < analyzeCount; i++) {
            try {
                // 按“单图”独立分析，便于部分失败时给出降级结果。
                String singleResult = analyzeSingleImage(decodedImages.get(i), medicalContext, i + 1);
                if (singleResult != null && !singleResult.isBlank()) {
                    results.add(singleResult);
                }
            } catch (Exception e) {
                log.warn("MCP 图片分析第 {} 张失败: {}", i + 1, e.getMessage());
                results.add("[图片 " + (i + 1) + " 分析失败]");
            }
        }

        return String.join("\n---\n", results);
    }

    private String analyzeSingleImage(byte[] imageBytes, String medicalContext, int index) {
        // 当前统一按 JPEG 媒体类型提交；上游只要是有效图片字节即可由模型理解。
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG,
                new ByteArrayResource(imageBytes));

        String prompt = buildVisionPrompt(medicalContext, index);
        String result = visionChatClient.prompt()
                .user(u -> u.text(prompt).media(media))
                .call()
                .content();

        log.info("MCP 图片分析 [{}] 完成, 结果长度={}", index,
                result != null ? result.length() : 0);
        return result;
    }

    private String buildVisionPrompt(String medicalContext, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("作为医学影像辅助分析助手，请分析第 ").append(index).append(" 张图片：\n");
        sb.append("1. 描述图片中可见的关键信息（如症状外观、检查报告内容等）\n");
        sb.append("2. 如果是检查报告，提取关键指标和异常项\n");
        sb.append("3. 如果是症状照片，描述可见特征\n");
        sb.append("4. 给出初步观察建议（非临床诊断）\n");
        if (medicalContext != null && !medicalContext.isBlank()) {
            sb.append("\n患者上下文信息：\n").append(medicalContext);
        }
        return sb.toString();
    }

    /**
     * 解码 Base64 图片，支持 data: URL 格式和纯 base64 字符串。
     */
    private DecodedImage decodeImage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        Matcher matcher = DATA_URL_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String base64Data = matcher.group(2);
            try {
                return new DecodedImage(Base64.getDecoder().decode(base64Data));
            } catch (IllegalArgumentException e) {
                log.warn("MCP 图片解码失败（data URL）: {}", e.getMessage());
                return null;
            }
        }
        // 纯 base64（不带 data:image 前缀）
        try {
            return new DecodedImage(Base64.getDecoder().decode(trimmed));
        } catch (IllegalArgumentException e) {
            log.warn("MCP 图片解码失败（纯 base64）: {}", e.getMessage());
            return null;
        }
    }

    private record DecodedImage(byte[] data) {}
}
