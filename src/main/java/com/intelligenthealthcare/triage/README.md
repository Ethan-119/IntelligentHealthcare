# triage 模块说明

智能导诊限界上下文：AI 辅助分析、多轮对话会话管理、槽位状态跟踪与 MCP 外部服务集成预留。

## 职责边界

- **api**：`AiAnalysisController`（`/api/ai`）暴露 AI 分析端点，接收文本与图片。
- **application**：`AiAnalysisService` 接口 + `AiAnalysisServiceImpl` 实现，编排分析流程并通过 Spring AI ChatClient 调用大模型。
- **domain**：`TriageSession`（导诊会话）、`TriageSlotState`（槽位状态）、`TriageTurn`（对话轮次）的核心模型。
- **infrastructure**：MyBatis-Plus Mapper 提供持久化接入。

## 主要 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/ai/analyze` | AI 辅助分析，接收 `AiAnalysisRequest`（文本 + 可选图片 base64），返回 `AiAnalysisResponse` |

## AI 分析流程

```
用户输入（文本 + 可选图片）
    │
    ▼
AiAnalysisController.analyze()
    │
    ▼
AiAnalysisServiceImpl.analyze(content, images)
    │
    ├── 纯文本 → ChatClient.prompt(prompt).call() → 返回结构化分析
    │
    └── 含图片 → [MCP 集成预留] → 外部视觉服务分析图片
                 → ChatClient 综合文本 + 图片分析结果 → 返回综合诊断
```

## MCP 集成预留

当用户症状描述模糊时，系统可引导用户上传图片（症状照片、检查报告等）。本模块本身不具备视觉分析能力，通过 MCP 协议调用外部服务：

- **入口**：`AiAnalysisServiceImpl.analyze()` 中 `hasImages` 分支
- **职责**：将 base64 图片通过 MCP 转发给外部视觉服务（如 DashScope 多模态、第三方医学影像服务）
- **结果**：外部返回的图片分析结果与 ChatClient 文本分析合并，构成最终 `AiAnalysisResponse`

MCP 在本系统中的角色是**能力边界的扩展层**——当本地 ChatClient 仅处理文本时，MCP 作为桥梁连接外部专业能力。

## 领域模型

### TriageSession（导诊会话）
一次完整的导诊对话，记录用户 ID、当前阶段、询问轮次、患者年龄/性别、严重程度、推荐路径等。

### TriageSlotState（槽位状态）
导诊过程中的知识槽位快照：症状摘要、匹配的疾病/医院/科室/医生、缺失槽位列表。

### TriageTurn（对话轮次）
单轮对话记录：用户原始消息、规范化查询、意图识别、阶段标记、系统回复文本、原始决策 JSON。

## 与其它模块的协作

- **rag**：AI 分析可结合 RAG 向量检索结果增强诊断建议。
- **knowledge**：疾病、医院、科室、医生等基础数据供分析结果引用。
- **audit**：AI 召回/分析结果可落审计日志（`AiRecallAuditLog`）。
- **auth**：用户会话通过 JWT 身份关联。

## 配置要点

- AI 模型配置见 `application.yml` 中 `spring.ai.openai` 段（当前对接 DashScope 兼容模式，模型 `qwen-plus`）。
- 图片分析 MCP 服务地址/凭证后续通过配置扩展。
