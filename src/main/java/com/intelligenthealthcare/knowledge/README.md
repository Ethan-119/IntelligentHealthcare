# knowledge 模块说明

本模块负责医疗知识库基础数据（疾病、医疗能力、医院、科室、医生、关系映射）的实体与持久化接入，并提供查询缓存能力。

## 当前生效口径

- **PostgreSQL**：结构化主数据与关系数据（疾病、医院、科室、医生、能力关系等）。
- **MongoDB**：RAG 文档块与向量数据（`rag_document_chunks`）。
- **Redis/Redisson**：知识库热点缓存（`knowledge:*`）与会话相关缓存。

## 缓存更新策略（当前）

- 读路径：先查 Redis（Redisson `RMapCache`），未命中再查 PostgreSQL 并回填缓存。
- 写路径：由管理员触发（如导入任务）先更新 PostgreSQL，成功后删除 knowledge 缓存。
- 自动刷新：离峰时段（默认 00:00-03:00）定时预热热点缓存。
- 人工刷新：管理员页面可手动清理/刷新缓存。

## 关于分页（本阶段决策）

- 目前 `knowledge` 查询接口**暂不做分页改造**，保持现有返回结构，优先保证联调与业务闭环。
- 后续当数据量增长到影响接口时延、响应体大小或缓存体积时，再统一切换为分页接口。
- 分页改造时将同步调整缓存 key 设计（按页缓存），避免全量大对象缓存。

## 模块定位（请务必遵守）

- `knowledge.domain.model` 是基础数据载体，不承载复杂业务规则。
- 导入、审核、临床决策逻辑放在对应业务上下文（如 `importjob`、`triage`）。
- 不在其他模块重复定义 knowledge 实体。

## 包结构

- `domain.model`：表映射实体。
- `infrastructure.persistence`：MyBatis Mapper 持久化接入。
- `api`：知识库查询与缓存管理接口。
- `application`：查询缓存、缓存刷新调度等应用服务。
