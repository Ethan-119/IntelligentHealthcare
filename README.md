# Intelligent Healthcare

基于 Spring AI 的智能医疗导诊系统，提供 AI 分诊对话、RAG 医学知识检索、医院/医生推荐等功能。

## 技术栈

| 层级     | 技术                                 | 说明                                 |
| -------- | ------------------------------------ | ------------------------------------ |
| 框架     | Spring Boot 3.4.4 + Java 17          | 主应用框架                           |
| AI       | Spring AI 1.1.5 + MCP                | LLM 调用、工具编排、模型上下文协议   |
| LLM      | DashScope (qwen-plus / qwen-vl-plus) | 通义千问 / 千问视觉，OpenAI 兼容协议 |
| 数据库   | PostgreSQL                            | 业务数据                           |
| 文档库   | MongoDB                              | 知识文档、embedding 向量             |
| 缓存     | Redis + Redisson 3.52.0              | 分布式缓存、分布式锁                 |
| ORM      | MyBatis-Plus 3.5.9                   | 数据访问层                           |
| 认证     | JWT (jjwt 0.12.6) + jBCrypt          | token 鉴权 + 密码哈希                |
| 文档解析 | Apache POI + OpenCSV + PDFBox        | Excel / CSV / PDF 知识库导入         |

## 功能模块

### AI 导诊对话 (`triage`)

- 医疗问题判定（非医疗问题拒绝）
- ReAct Agent 推理循环：LLM 自主决策 → 工具调用 → 观察结果 → 循环
- 流式 SSE 输出（`text/event-stream`）
- 多轮对话上下文管理 + 槽位状态追踪
- 会话历史自动缓存（Redis）

### 知识库管理 (`knowledge`)

- 疾病、医院、科室、医生、能力标签 CRUD
- 多级 Redis 缓存 + 逻辑过期 + 防击穿分布式锁
- 凌晨低峰期自动缓存刷新

### RAG 医学检索 (`rag`)

- 文档摄入：Excel / CSV / PDF 批量导入，自动分块 + embedding
- 向量检索：MongoDB 存储 1024 维向量，L2 距离排序
- 热缓存优先：高频查询直接走 Redis，未命中回落向量库
- 文档上下架开关

### 用户系统 (`auth` / `patient`)

- 手机号 + 密码注册 / 登录
- JWT 服务端存储校验，删除即登出
- 患者档案管理

### Agent 工具集

| 工具                       | 说明                              |
| -------------------------- | --------------------------------- |
| `SEARCH_MEDICAL_KNOWLEDGE` | RAG 医学知识检索                  |
| `RECOMMEND_HOSPITAL`       | Haversine 距离 + 等级推荐医院     |
| `RECOMMEND_DOCTOR`         | 科室匹配 + 权威分排序             |
| `LOG_EMERGENCY`            | 急症预警审计日志                  |
| `SESSION_MEMORY`           | 跨轮对话记忆回查                  |
| `PATIENT_PROFILE`          | 用户档案读取                      |
| `imageVisionTool`          | MCP 图片视觉分析（皮肤病/影像等） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL（含 pgvector 扩展）
- MongoDB
- Redis
- 阿里云 DashScope API Key

### 启动

```bash
# 1. 配置 application-dev.yml 中的数据库连接和 API Key
# 2. 编译
mvn -DskipTests compile

# 3. 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 关键 API

| 端点                                 | 方法 | 说明                    |
| ------------------------------------ | ---- | ----------------------- |
| `/api/auth/register`                 | POST | 用户注册                |
| `/api/auth/login`                    | POST | 用户登录                |
| `/api/triage/chat/stream`            | POST | 流式 AI 导诊对话（SSE） |
| `/api/triage/chat`                   | POST | 非流式 AI 导诊对话      |
| `/api/triage/sessions`               | GET  | 查询用户会话列表        |
| `/api/knowledge/{type}`              | GET  | 知识库查询              |
| `/api/rag/search`                    | POST | RAG 语义检索            |
| `/api/rag/document/ingest`           | POST | 知识文档摄入            |
| `/api/admin/knowledge/cache/refresh` | POST | 手动刷新缓存            |

---

## AI 导诊请求流程

```
用户输入症状描述（如 "头痛发烧咳嗽三天"）
    │
    ▼
① 关键词判定 + ML 分类（isMedicalQuery）
    非医疗问题 → 直接拒绝，不创建会话
    医疗问题 → 继续
    │
    ▼
② 会话与上下文准备
    getOrCreateSession → 创建/复用会话
    listRecentTurns → Redis 缓存 / DB 查历史
    autoBackfillProfileIfMissing → 档案回填
    │
    ▼
③ [可选] MCP 图片视觉分析
    ImageVisionTools → DashScope qwen-vl-plus
    逐张分析医学图片，结果嵌入后续 Prompt
    │
    ▼
④ ReAct Agent 推理循环（最多 N 步）
    planNextAction → LLM 决策下一步动作
    executeAction → 执行工具调用
    action=FINISH → 退出循环
    │
    ▼
⑤ LLM 最终答复（DashScope qwen-plus）
    流式（SSE）：ChatClient.stream() 逐 token
    非流式：ChatClient.call() 一次性返回
    │
    ▼
⑥ 持久化 + 缓存写入
    INSERT triage_turn / UPDATE triage_session
    Redis RList ai:session:history:{id}（最近 6 轮，TTL 20min）
    │
    ▼
  返回给用户
```

## 缓存分层策略

```
第一层：Redis（会话历史）
  ├─ Key: ai:session:history:{sessionId}（Redisson RList）
  ├─ TTL: 20 分钟
  └─ 作用: 避免每次对话都查 triage_turn 表

第二层：Redis（知识库基础数据）
  ├─ Key: knowledge:disease / knowledge:hospital / knowledge:department
  ├─ TTL: 30-60 分钟（逻辑过期 + 物理 TTL 倍数兜底）
  └─ 作用: 减少 PostgreSQL 查询，缓存击穿保护（分布式锁刷新）

第三层：Redis（RAG 热点知识）
  ├─ Key: knowledge:rag:hotChunk（Redisson RMapCache）
  ├─ TTL: 60 分钟（每个 field 独立 TTL）
  └─ 作用: 加速 ReAct Agent SEARCH_MEDICAL_KNOWLEDGE 动作

第四层：Redis（JWT 登录态）
  ├─ Key: auth:token:patient:{patientId}（String）
  ├─ TTL: 8 小时
  └─ 作用: 服务端 token 校验，删除即登出

第五层：MongoDB 向量库（知识底座，不做缓存用）
  ├─ 存什么: 疾病知识文档的 1024 维 embedding 向量
  └─ 作用: 语义检索的原材料
```

### 什么不缓存

| 步骤             | 缓存？ | 原因                             |
| ---------------- | ------ | -------------------------------- |
| Embedding 向量化 | 否     | 向量不是业务结果                 |
| MongoDB 向量检索 | 否     | 每个问题向量不同，结果动态       |
| Prompt 组装      | 否     | 中间过程，会话历史已缓存最终回答 |
| LLM 调用         | 否     | 医疗场景每次输入不同，命中率极低 |

### Redis Key 规范

```
auth:token:patient:{patientId}          → String  —— JWT 登录态
ai:session:history:{sessionId}          → List    —— 会话最近 6 轮（Redisson RList）
knowledge:disease                       → Hash    —— 疾病主数据（RMapCache, 60min TTL）
knowledge:hospital                      → Hash    —— 医院数据（60min TTL）
knowledge:department                    → Hash    —— 科室数据（30min TTL）
knowledge:doctor                        → Hash    —— 医生数据（30min TTL）
knowledge:capability                    → Hash    —— 能力标签（30min TTL）
knowledge:rag:hotChunk                  → Hash    —— RAG 热点知识（每个 field 60min TTL）
```

### 缓存刷新时机

| 事件           | 操作                                                         |
| -------------- | ------------------------------------------------------------ |
| 管理员手动刷新 | `POST /api/admin/knowledge/cache/refresh` → 清空全部知识缓存并重载 |
| 管理员清除缓存 | `POST /api/admin/knowledge/cache/evict` → 清空全部知识缓存   |
| 定时任务       | `KnowledgeCacheRefreshScheduler` 凌晨低峰期每 30 分钟自动刷新 |
| 会话历史       | TTL 20 分钟自动失效                                          |
| JWT Token      | TTL 8 小时，登录时覆盖旧 token                               |

---

## 当前已知问题（2026-05-21）

- RAG 检索为全量读取 Mongo 后在内存做 L2 距离排序，数据量增大后会有性能瓶颈。
- 知识库导入为同步执行，大文件可能阻塞请求。
- 外部依赖（PostgreSQL / Redis / MongoDB / AI Key）任一不可用会导致关键业务降级。

---

## License

MIT
