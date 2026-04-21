package com.intelligenthealthcare.ai.service.impl;

import com.intelligenthealthcare.ai.service.AiAnalysisService;
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
    public String analyze(String content) {
        if (!StringUtils.hasText(content)) {
            return "输入内容为空，无法分析。";
        }

        String prompt = """
                你是医疗辅助分析助手。请基于以下内容给出结构化分析：
                1. 关键信息提取
                2. 初步风险提示
                3. 建议下一步检查方向
                4. 说明这不是最终临床诊断

                待分析内容：
                %s
                """.formatted(content);

        return chatClient.prompt(prompt).call().content();
    }
}
