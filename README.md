# Intelligent Healthcare

## 当前已知问题

本节记录 2026-05-21 的状态。

### 1) 启动与构建状态

- `mvn -DskipTests compile` 通过。
- `mvn test` 通过，但 `src/test/java` 无业务测试用例，无法代表业务正确性。
- `application.yml` 中 `spring.data` 重复键问题已修复，应用可正常启动。

### 2) 业务链路待关注项

- RAG 检索实现存在扩展性风险：
  - 当前实现为全量读取 Mongo 文档后在内存计算 L2 距离并排序，数据量增大后可能出现性能瓶颈。
- 导入任务为同步执行：
  - 上传接口内直接执行文件解析与逐行导入，较大文件场景可能导致请求阻塞或超时。
- 外部依赖敏感：
  - PostgreSQL、Redis、MongoDB、AI 密钥任一异常都可能导致关键业务不可用或功能降级。

### 3) 建议处理顺序

1. 补充集成测试（至少覆盖登录、鉴权、AI 分析、RAG）。
2. 逐步完善异步化导入能力、RAG 向量索引优化。

---

## AI 导诊缓存架构

### 请求流程（RAG + Redis 缓存）

```
用户输入症状描述（如 "头痛发烧咳嗽三天"）
    │
    ▼
┌─────────────────────────────────────────────┐
│  ① 关键词判定 + ML 分类（isMedicalQuery）      │
│     非医疗问题 → 直接拒绝，不创建会话           │
│     医疗问题 → 继续 ②                         │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│  ② 会话与上下文准备                           │
│     getOrCreateSession → 创建/复用会话        │
│     listRecentTurns → Redis 缓存 / DB 查历史  │
│     autoBackfillProfileIfMissing → 档案回填   │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│  ③ [可选] MCP 图片视觉分析                     │
│     ImageVisionTools → DashScope qwen-vl-plus │
│     逐张分析医学图片，结果嵌入后续 Prompt       │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│  ④ ReAct Agent 推理循环（最多 N 步）           │
│     planNextAction → LLM 决策下一步动作        │
│     executeAction → 执行工具调用：             │
│       SEARCH_MEDICAL_KNOWLEDGE → RAG 向量检索 │
│       RECOMMEND_HOSPITAL → Haversine 距离排序  │
│       RECOMMEND_DOCTOR → 科室匹配+权威分       │
│       LOG_EMERGENCY → 急症预警落审计日志       │
│       SESSION_MEMORY → 回查对话槽位            │
│       PATIENT_PROFILE → 读取用户档案           │
│     action=FINISH → 退出循环，进入最终答复      │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│  ⑤ LLM 最终答复（DashScope qwen-plus）         │
│     流式（SSE）：ChatClient.stream() 逐 token  │
│     非流式：ChatClient.call() 一次性返回       │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────┐
│  ⑥ 持久化 + 缓存写入                          │
│     INSERT triage_turn（对话轮次）             │
│     UPDATE triage_session（会话状态）          │
│     INSERT/UPDATE triage_slot_state（槽位）    │
│     INSERT ai_recall_audit_log（审计日志）     │
│     Redis RList ai:session:history:{id}       │
│       → 最近 6 轮，TTL 20 分钟                 │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
                 返回给用户
```

### 缓存分层策略

```
第一层：Redis 缓存（会话历史）
  ├─ Key: ai:session:history:{sessionId}（Redisson RList）
  ├─ TTL: 20 分钟
  ├─ 存什么: 最近 6 轮对话（turnNo + userMessage + replyText JSON）
  └─ 作用: 避免每次对话都查 triage_turn 表

第二层：Redis 缓存（知识库基础数据）
  ├─ Key: knowledge:disease / knowledge:hospital / knowledge:department
  │        knowledge:doctor / knowledge:capability（Redisson RMapCache）
  ├─ TTL: 30-60 分钟（逻辑过期 + 物理 TTL 倍数兜底）
  ├─ 存什么: 疾病字典、医院列表、科室、医生、能力标签
  └─ 作用: 减少 PostgreSQL 查询，缓存击穿保护（分布式锁刷新）

第三层：Redis 缓存（RAG 热点知识）
  ├─ Key: knowledge:rag:hotChunk（Redisson RMapCache）
  ├─ TTL: 60 分钟（每个 field 独立 TTL）
  ├─ 存什么: 高频访问的医学知识片段
  └─ 作用: 加速 ReAct Agent 的 SEARCH_MEDICAL_KNOWLEDGE 动作

第四层：Redis 缓存（JWT 登录态）
  ├─ Key: auth:token:patient:{patientId}（String）
  ├─ TTL: 8 小时
  ├─ 存什么: 用户当前有效 JWT
  └─ 作用: 服务端 token 校验，删除即登出

第五层：MongoDB 向量库（知识底座，不做缓存用）
  ├─ 存什么: 疾病知识文档的 1024 维 embedding 向量
  └─ 作用: 语义检索的原材料，每次新问题都要查（当前为内存 L2 距离计算）
```

### 什么地方不需要缓存

| 步骤 | 是否缓存 | 原因 |
|------|----------|------|
| Embedding 向量化 | 不缓存 | 同一句话的向量结果固定，但向量本身不是业务结果，没必要单独存 |
| MongoDB 向量检索 | 不缓存检索过程 | 每个问题向量不同，检索结果是动态的 |
| Prompt 组装 | 不缓存 | 是中间过程，缓存最终回答无意义（会话历史已缓存） |
| LLM 调用 | 不缓存单次调用 | 医疗场景每次输入不同，缓存命中率极低 |

### Redis Key 规范

```
auth:token:patient:{patientId}          → String  —— JWT 登录态
ai:session:history:{sessionId}          → List    —— 会话最近 6 轮缓存（Redisson RList）
knowledge:disease                       → Hash    —— 疾病主数据（RMapCache, 60min TTL）
knowledge:hospital                      → Hash    —— 医院数据（60min TTL）
knowledge:department                    → Hash    —— 科室数据（30min TTL）
knowledge:doctor                        → Hash    —— 医生数据（30min TTL）
knowledge:capability                    → Hash    —— 能力标签（30min TTL）
knowledge:rag:hotChunk                  → Hash    —— RAG 热点知识（每个 field 60min TTL）
```

### 缓存刷新时机

| 事件 | 操作 |
|------|------|
| 管理员手动刷新 | POST `/api/admin/knowledge/cache/refresh` → 清空全部知识缓存并从 DB 重载 |
| 管理员清除缓存 | POST `/api/admin/knowledge/cache/evict` → 清空全部知识缓存 |
| 定时任务 | `KnowledgeCacheRefreshScheduler` 凌晨低峰期每 30 分钟自动刷新 |
| 会话历史 | TTL 20 分钟自动失效，下次对话从 DB 重载 |
| JWT Token | TTL 8 小时，登录时覆盖旧 token |

---

## 分布式锁 TODO

> 当前项目已引入 Redisson（`redisson-spring-boot-starter 3.52.0`），
> 知识库缓存已使用 `RLock` 做防击穿保护（`KnowledgeQueryApplicationService`）。
> 以下场景仍有并发竞争风险，需按优先级逐步加锁。

### 场景汇总

| 优先级 | 场景 | 锁 Key 模式 | 影响 |
|--------|------|------------|------|
| P0 | 分诊会话计数器 | `lock:triage:session:{sessionId}` | askRound/invalidAnswerCount 读-改-写无锁，可能丢失更新 |
| P0 | 用户注册重复 | `lock:register:phone:{phone}` | existsByPhone 检查后并发插入（DB 已有 UNIQUE 约束兜底） |
| P1 | 多管理员刷新缓存 | `lock:cache:knowledge:refresh` | 管理员 + 定时任务同时刷新，缓存状态不一致 |
| P2 | 分诊会话重复创建 | `lock:triage:session:create:{userId}:{sessionId}` | 并发 insert 可能产生重复会话 |
| P2 | 分诊槽位丢失更新 | `lock:triage:slot:{sessionId}` | upsertSlotState 的 select + insert/update 无锁 |
| P2 | RAG 摄取重复文档 | `lock:rag:ingest:{sourceType}:{sourceId}:{chunkKey}` | MongoDB 无唯一约束，并发 upsert 可能重复 |
| P3 | 患者资料丢失更新 | `lock:patient:profile:{patientId}` | updateById 无条件覆盖，后写胜出 |

### 处理顺序建议

1. P0 场景加 Redisson 分布式锁
2. P1 场景加锁或改为单一定时任务
3. P2 场景根据实际并发量决定用分布式锁还是乐观锁
4. P3 视业务冲突频率决定是否投入
