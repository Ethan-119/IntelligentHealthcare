# knowledge 模块说明

本模块提供**医疗基础知识与向量检索**相关的**表映射实体**与 **MyBatis Mapper**，作为全库共享的**基础数据层**。

## 定位（请务必遵守）

- 疾病、医院、科室、医生、能力关系等实体属于**基础数据**，用于落库、查询与**向量字段**维护。
- 这些类**不**作为承载业务规则、领域行为、用例流程的聚合根；**不要**在 `knowledge.domain.model` 中新增业务方法或复杂领域逻辑。
- 与导入、审核、临床决策相关的行为应放在对应业务模块（例如 `importjob` 中解析与策略，通过 `import` 使用本模块实体，而非在本模块内堆逻辑）。

## 包结构（当前）

- `domain.model`：与表结构对应的实体（Lombok 等可保留为数据类写法）。
- `infrastructure.persistence`：各表 `Mapper` 及 MyBatis 接入。

已移除无代码占位的空包：`api`、`application`、`domain.repository`（原仅 `package-info`）。

## 相关

- 表定义见 `src/main/resources/db/init/CreateTable/Create.sql` 等。
