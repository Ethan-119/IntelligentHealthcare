# importjob 模块说明

知识库批量导入限界上下文：任务记录、行级失败日志、需人工审核项，以及 Excel/CSV 解析。

## DDD 分层与命名

- **应用服务**（`application.ImportJobApplicationService`）：负责用例编排与事务边界；不放业务规则，不直接依赖 MyBatis `Mapper`，只依赖 `domain.repository` 接口。
- **领域服务**（`domain.service`）：`DiseaseMasterImportPolicy`、`DiseaseAliasImportPolicy` 负责解析与规则决策。
- **仓储**（`domain.repository` + `infrastructure` 实现）：`ImportJobRecordRepository`、`ImportFailureLogRepository`、`ImportReviewItemRepository`、`KnowledgeImportRepository`。

## 职责边界

- **api**：`ImportJobController`（`/api/admin/import/jobs`）上传、任务列表/详情、失败行、审核项。
- **application**：只做编排，持久化仅通过仓储接口。
- **domain**：任务进度、失败日志、审核项、解析决策结果等核心模型。
- **infrastructure**：`ImportFileTableReader` 与 `Mybatis*Repository` 仓储实现。

## 主要 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/admin/import/jobs` | `multipart`：`file`、`datasetType` |
| `GET` | `/api/admin/import/jobs` | 最近任务列表 |
| `GET` | `/api/admin/import/jobs/{id}` | 任务详情 |
| `GET` | `/api/admin/import/jobs/{id}/failures` | 失败记录 |
| `GET` | `/api/admin/import/jobs/{id}/review-items?resolved=` | 审核项列表 |
| `PUT` | `/api/admin/import/jobs/review-items/{reviewId}/resolve` | 标记审核项已处理 |

## 数据落库口径（更正）

- `import_job_record`、`import_failure_log`、`import_review_item`：业务任务与过程数据，归 PostgreSQL。
- 疾病/医院/科室/医生等知识库主数据：按当前口径归 MongoDB（向量知识库 + 文档存储）。
- Redis 后续再接入（缓存/会话增强）。
