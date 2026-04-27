# importjob 模块说明

知识库**批量导入**限界上下文：任务记录、行级失败日志、需人工审核项，以及 Excel/CSV 解析。领域规则不写在应用服务中，而放在 `domain` 的策略、值对象与工厂。

## DDD 分层与命名

- **应用服务**（`application.ImportJobApplicationService`）：在经典 DDD 中即 **Application Service**，负责**用例编排**与事务意图边界；**不**放业务规则，也**不**直接依赖 MyBatis/Mapper，只依赖 **domain 仓储接口**。
- **领域服务**（`domain.service`）：`DiseaseMasterImportPolicy` 等，承载无法单独归属某一实体上的规则（策略）。
- **仓储**（`domain.repository` + `infrastructure` 实现）：`ImportJobRecordRepository`、`ImportFailureLogRepository`、`ImportReviewItemRepository` 对应本限界上下文的表；`KnowledgeImportRepository` 表示对**知识库**上下文的读写在领域侧的抽象（可视为防腐/仓储合一，由 `MybatisKnowledgeImportRepository` 实现）。

## 职责边界

- **api**：`ImportJobController`（路径前缀 `/api/admin/import/jobs`）上传、任务列表/详情、失败行、待审核列表、将待办标为已解决。
- **application**：`ImportJobApplicationService` 只做编排与调用；持久化**仅通过** `domain.repository` 的实现类完成。
- **domain**：
  - **model**：`ImportTableRow`、`ImportJobProgress` 等本模块内对象；**不**复制 `knowledge` 的实体，业务表行直接解析/组装为 `com.intelligenthealthcare.knowledge.domain.model` 下实体（`DiseaseMaster`、`DiseaseAlias` 等）并在策略类中使用；`ImportFailureLog`、`ImportReviewItem` 等实体的领域工厂/行为（如 `fromLineError`、`markResolvedWithNote`）。
  - **service**：`DiseaseMasterImportPolicy`、`DiseaseAliasImportPolicy`（**解析 +** 去重/是否进待办等规则；解析结果以内嵌 `ParsedRow` 携带，不另建与知识实体平行的 `*ImportLine` 类）。
  - **repository**：仓储**接口**（本模块 + 对知识库导入的 `KnowledgeImportRepository`）。
  - **ImportJobFactory**：创建运行中任务、文件名安全截断。
- **infrastructure**：`ImportFileTableReader`；`infrastructure/persistence` 下 `Mybatis*Repository` 实现各仓储；底层仍使用 MyBatis `Mapper`（仅实现类可见）。

## 主要 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/admin/import/jobs` | `multipart`：`file`、`datasetType`（见下表） |
| `GET`  | `/api/admin/import/jobs` | 最近任务列表（最多 200 条） |
| `GET`  | `/api/admin/import/jobs/{id}` | 单任务详情 |
| `GET`  | `/api/admin/import/jobs/{id}/failures` | 该任务失败行 |
| `GET`  | `/api/admin/import/jobs/{id}/review-items?resolved=` | 待审核/已办（`resolved` 可选） |
| `PUT`  | `/api/admin/import/jobs/review-items/{reviewId}/resolve` | body：`{ "resolutionNote": "..." }` |

**注意**：与项目其它受保护 API 一样需登录；**管理员**与患者账号的进一步区分可后续在配置或角色上收紧。

## datasetType 与表头

| 取值 | 目标表 / 行为 |
|------|----------------|
| `DISEASE_MASTER` | `disease_master`；**必填**列：`disease_code`、`disease_name`；可选如 `symptom_keywords`、`gender_rule`、`age_min`、`age_max` 等（列名不区分大小写，解析为小写） |
| `DISEASE_ALIAS` | `disease_alias`；**必填**：`disease_code`、`alias_name`；`disease_code` 须能在未删除的 `disease_master` 中解析到 |

**文件格式**：首行为表头；同编码已存在且名称一致视为成功跳过；同编码已存在但名称不一致则写入**待审核**（`import_review_item`）而非强行覆盖。

## 任务状态（`import_job_record.status`）

- `RUNNING`：处理中；结束后为 `SUCCESS`（无失败/无待办）、`PARTIAL_SUCCESS`（有失败或待办）、`FAILED`（整文件无法解析等）。

## 配置与依赖

- `spring.servlet.multipart`：大文件上传上限见根目录 `application.yml`。
- Excel/CSV 依赖见根目录 `pom.xml`（如 Apache POI、OpenCSV）。

## 相关表

- `import_job_record`、`import_failure_log`、`import_review_item`；知识数据写入 `disease_master` / `disease_alias`（表结构见 `db/init/CreateTable/Create.sql`）。
