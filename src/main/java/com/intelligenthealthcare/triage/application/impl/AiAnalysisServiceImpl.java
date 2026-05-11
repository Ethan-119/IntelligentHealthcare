package com.intelligenthealthcare.triage.application.impl;

import com.intelligenthealthcare.triage.application.AiAnalysisService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final ChatClient chatClient;

    public AiAnalysisServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String analyze(String content, List<String> images) {
        if (!StringUtils.hasText(content)) {
            return "输入内容为空，无法分析。";
        }

        boolean hasImages = images != null && !images.isEmpty();

        // TODO: MCP 集成 — 调用外部视觉服务分析图片，结果合并到最终输出
        if (hasImages) {
            // 后续通过 MCP 调用 DashScope 多模态 / 第三方医学影像服务
            // String imageAnalysis = mcpVisionClient.analyze(images, context);
        }

        String prompt = buildPrompt(content, hasImages);

        return chatClient.prompt(prompt).call().content();
    }

    private String buildPrompt(String content, boolean hasImages) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是医疗辅助分析助手。请基于以下内容给出结构化分析：
                1. 关键信息提取
                2. 初步风险提示
                3. 建议下一步检查方向
                4. 说明这不是最终临床诊断
                """);
        if (hasImages) {
            sb.append("5. 用户已上传图片，图片分析结果将由外部视觉服务补充，请结合文本描述给出初步分析\n");
        }
        sb.append("\n待分析内容：\n").append(content);
        return sb.toString();
    }
}
