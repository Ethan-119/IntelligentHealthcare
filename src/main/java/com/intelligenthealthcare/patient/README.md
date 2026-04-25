# patient 模块说明

患者聚合：账号所在行的领域模型、个人资料与导诊相关上下文的读改，及与持久化的仓储接口。

## 职责边界

- **api**：`PatientProfileController` 提供「我的资料」查询与更新（需登录）。
- **application**：`PatientProfileApplicationService` 用例编排：按主键加载患者、应用层跨实体规则（如手机号唯一）、调用领域实体的 `updateProfile` 并保存。
- **domain**：
  - **model**：`Patient` 聚合根（`updateProfile` 等**领域行为**内聚在实体上）；`Gender`、`TriagePrefer` 等枚举/值对象。
  - **exception**：`PatientNotFoundException`、`PatientPhoneAlreadyUsedException`、`PatientPhoneRequiredException` 等，由 `shared` 的 `GlobalExceptionHandler` 映射为 HTTP 状态与 JSON `message`。
  - **repository**：`PatientRepository` 由基础设施实现，屏蔽 MyBatis 细节。

## 主要 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET`  | `/api/patient/me`     | 当前患者资料（与 `/api/auth/me` 可并存，以本模块为档案域主入口时以此为准） |
| `PUT`  | `/api/patient/me`     | 更新个人资料，请求体为 `UpdateMyProfileRequest` |

均需在请求头携带 `Authorization: Bearer <accessToken>`。

## 业务规则（摘要）

- 更新资料时若修改手机号，应用层校验全库唯一性；冲突时抛出 `PatientPhoneAlreadyUsedException`。
- 资料字段校验失败由 Bean Validation 与全局异常处理统一返回；领域规则失败抛领域异常。

## 与 auth 的关系

- 身份以 JWT 中的患者 ID 为准；`PatientAuthPrincipal` 由 **auth** 在过滤器中填充。
- 若需区分「未找到当前 ID 对应患者」等场景，使用本模块领域异常，避免在应用层写 HTTP 相关代码。

## 相关表

- 表名 `patient`：主键、手机号唯一、档案与导诊偏好等，详见 `db/init/CreateTable/Create.sql`。
