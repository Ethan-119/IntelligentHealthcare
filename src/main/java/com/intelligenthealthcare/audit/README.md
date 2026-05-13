# Audit 模块

## 当前状态

模块已接入 AI 分析调用链，可记录审计日志。

### 已实现

| 层 | 文件 | 说明 |
|---|------|------|
| Domain Model | `domain/model/AiRecallAuditLog.java` | AI 召回审计日志实体，映射 `ai_recall_audit_log` 表 |
| Infrastructure | `infrastructure/persistence/AiRecallAuditLogMapper.java` | MyBatis-Plus BaseMapper，提供 CRUD |
| Domain Repository | `domain/repository/AiRecallAuditLogRepository.java` | 审计日志领域仓储接口 |
| Infrastructure Repository | `infrastructure/persistence/MybatisAiRecallAuditLogRepository.java` | 基于 Mapper 的仓储实现 |
| Application Service | `application/AuditApplicationService.java` | 对外提供写审计日志能力 |
| Business Integration | `triage/application/impl/AiAnalysisServiceImpl.java` | AI 分析成功/失败后落审计日志（失败不阻断主流程） |

### 只有空占位符

- `api/package-info.java` — 无 Controller、无 DTO
- `application/package-info.java` — 无 ApplicationService
- `domain/repository/package-info.java` — 无领域仓库接口

## 待办事项

### 1. 领域层（domain）

- [ ] 定义 `AiRecallAuditLogRepository` 接口（`domain/repository/`）
  - 至少需要 `save`、`findByStatus`、`findByCreateTimeRange` 等查询方法
- [ ] 补充值对象（按需）：
  - `AuditStatus` 枚举（替代当前 `String status`）
  - 若 `ruleCandidateCodesJson` / `suggestedCodesJson` 有固定结构，定义对应的值对象替代裸 JSON 字符串

### 2. 基础设施层（infrastructure）

- [ ] 实现 `MybatisAiRecallAuditLogRepository`（`infrastructure/persistence/`）
  - 注入 `AiRecallAuditLogMapper`，实现领域仓库接口
  - 复杂查询用 MyBatis-Plus `LambdaQueryWrapper` 或自定义 XML
- [ ] 建表 DDL（或 Flyway/Liquibase 迁移脚本）

### 3. 应用层（application）

- [ ] 创建 `AuditApplicationService`
  - `recordAuditLog(...)` — 写入一条审计日志
  - `queryAuditLogs(...)` — 分页/条件查询
  - `getAuditDetail(Long id)` — 单条详情

### 4. API 层（api）

- [ ] 创建 `AuditController`（`@RestController`，路径如 `/api/audit`）
  - `POST /api/audit/query` — 分页查询审计日志列表
  - `GET /api/audit/{id}` — 查看单条详情
- [ ] 定义 DTO：
  - `AuditLogResponse` — 返回给前端的审计日志视图
  - `AuditLogQueryRequest` — 查询条件（时间范围、状态等）

### 5. 接入 RAG 模块

- `rag/domain/model/RagSourceType.java` 已定义 `AUDIT` 枚举值，但尚未接入
- [ ] 在 RAG 索引/检索逻辑中，将审计日志作为一种语料来源

### 6. 接入业务调用点

- [ ] 在 AI 召回/分诊流程的关键节点调用 `AuditApplicationService.recordAuditLog()` 落库
