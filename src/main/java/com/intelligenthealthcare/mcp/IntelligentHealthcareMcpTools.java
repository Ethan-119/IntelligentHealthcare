package com.intelligenthealthcare.mcp;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 工具入口：供后续将业务域能力以 {@link McpTool} 形式暴露给 MCP Host（如 IDE、其它 AI 应用）。
 * 当前仅提供最小示例，用于验证服务与协议链路与联调。
 */
@Service
public class IntelligentHealthcareMcpTools {

    @McpTool(description = "返回应用名称，用于确认 MCP 工具与后端已连通")
    public String getApplicationName() {
        return "intelligent-healthcare";
    }

    @McpTool(description = "回显一段文本，用于联调参数传递")
    public String echo(
            @McpToolParam(description = "任意简短文本", required = true) String message) {
        return message;
    }
}
