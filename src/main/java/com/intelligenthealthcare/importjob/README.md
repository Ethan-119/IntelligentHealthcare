# importjob 模块说明

知识库**批量导入**限界上下文：任务记录、行级失败日志、需人工审核项，以及 Excel/CSV 解析。领域规则不写在应用服务中，而放在 `domain` 的策略、值对象与工厂。

## 职责边界

- **api**：`ImportJobController`（路径前缀 `/api/admin/import/jobs`）上传、任务列表/详情、失败行、待审核列表、将待办标为已解决。
- **application**：`ImportJobApplicationService` 仅做编排：建任务、读文件、按行调领域结果并写库；持久化经 **防腐层** 访问知识表。
- **domain**：
  - **model**：`ImportTableRow`、`ImportJobProgress`、`DiseaseMasterImportLine` / `DiseaseAliasImportLine` 等值对象与解析结果；`ImportFailureLog`、`ImportReviewItem` 等实体的领域工厂/行为（如 `fromLineError`、`markResolvedWithNote`）。
  - **service**：`DiseaseMasterImportPolicy`、`DiseaseAliasImportPolicy`（去重、是否进待办等规则）。
  - **port**：`KnowledgeImportGateway` 接口，对领域屏蔽 MyBatis。
  - **ImportJobFactory**：创建运行中任务、文件名安全截断。
- **infrastructure**：`ImportFileTableReader`（首行表头、`.csv` / `.xlsx` / `.xls`）、`KnowledgeImportGatewayImpl`、各表 Mapper。

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
