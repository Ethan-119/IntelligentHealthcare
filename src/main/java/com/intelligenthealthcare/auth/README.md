# auth 模块说明

患者端认证与 JWT 会话：注册、登录、签发与解析访问令牌，并与 Spring Security 集成。

## 职责边界

- **api**：`AuthController` 暴露注册、登录、当前用户查询。
- **application**：`AuthService` 编排注册/登录/「当前用户」读库，事务边界在应用服务。
- **domain**：`PatientAuthPrincipal` 表示已认证患者（供控制器与下游模块注入）。
- **infrastructure**：`JwtService` 签发与解析；`JwtAuthenticationFilter` 从 `Authorization: Bearer` 头解析并写入 `SecurityContext`。
- **config**：`JwtConfig` / `JwtProperties` 绑定 `app.jwt.*`（`application.yml`）。

## 主要 HTTP 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/register` | 注册，返回 `TokenResponse`（`201`） |
| `POST` | `/api/auth/login` | 登录，返回 `TokenResponse` |
| `GET`  | `/api/auth/me` | 当前登录患者信息（需 JWT） |

未携带或无效 token 的受保护请求由 `shared` 的 `SecurityConfig` 统一返回 JSON `401`（与全局异常处理器的业务异常相区分）。

## 与其它模块的协作

- **patient** 等模块通过 `@CurrentPatient` + `CurrentPatientArgumentResolver`（`com.intelligenthealthcare.shared.security`）注入 `PatientAuthPrincipal`，避免在控制器中重复空登录判断。

## 配置要点

- `app.jwt.secret`：生产环境务必使用足够长度随机串，并通过环境变量覆盖。
- `app.jwt.expiration-ms`：与前端 token 展示及刷新策略需一致。

## 相关表

- 患者账号与基础档案存储于 `patient` 表，由本模块在注册/登录时读写字段，详细见 **patient 模块**文档。
