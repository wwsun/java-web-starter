# 脚手架架构说明

本文档面向使用此脚手架开发新项目的工程师，说明关键技术决策、分层规范和扩展方式。

---

## 定位与目标

java-web-starter 是一套**企业内部管理系统**的前后端分离脚手架，通过 fork/clone 快速启动新项目。
内置用户管理和认证能力，User 模块作为完整的架构示范，供新模块参考复制。

---

## 技术选型决策

| 选型 | 理由 |
|------|------|
| **MyBatis-Plus** 而非 JPA | 企业内部系统 SQL 通常需要精细控制；MyBatis-Plus 在保留 SQL 透明度的同时提供 CRUD 减负 |
| **JJWT + 双令牌方案** | Access Token 短期（1h）+ Refresh Token 长期（7d），无状态设计；JJWT 是 Java 生态最广泛的 JWT 库 |
| **Knife4j** 而非 Swagger UI | Knife4j 在 SpringDoc 基础上提供更好的中文界面和调试体验，适合内部系统 |
| **Spring Cache** | 抽象层封装，默认使用 `simple` (ConcurrentHashMap)，生产环境可选 **Redis** 7 |

---

## 分层架构

```
HTTP 请求
   ↓
Controller 层    职责：参数校验（@Valid）、调用 Service、返回 Result<XxxVO>
                 禁止：直接访问 Mapper、包含业务逻辑、返回 Entity
   ↓
Service 层       职责：业务逻辑、事务、缓存、异常抛出
                 禁止：直接构造 HTTP 响应、感知 SecurityContext（除获取用户名外）
   ↓
Mapper 层        职责：数据库访问，继承 BaseMapper<Entity>
                 禁止：业务逻辑

跨层禁止调用：Controller → Mapper（绕过 Service）
```

---

## 认证机制（双令牌）

```
登录 POST /api/auth/login
  → Spring Security 验证用户名密码
  → 生成 access_token（type=access，1h）+ refresh_token（type=refresh，7d）
  → 返回 TokenResponse

每次 API 请求
  → JwtAuthenticationFilter 提取 Bearer Token
  → validateAccessToken()：校验签名 + 未过期 + type == "access"
  → 设置 SecurityContext

Token 刷新 POST /api/auth/refresh（携带 refresh_token）
  → validateRefreshToken()：校验签名 + 未过期 + type == "refresh"
  → 生成新的 access_token + refresh_token
```

**type claim 校验是安全关键**：access token 不能用于刷新，refresh token 不能用于 API 认证。

---

## DTO / VO 规范

| 类型 | 用途 | 位置 |
|------|------|------|
| `XxxRequest` | 请求入参（@Valid 校验） | `<name>/dto/` |
| `XxxVO` | 响应出参（安全字段子集） | `<name>/` |
| `Entity` | 数据库映射 | `<name>/` |

转换入口：`XxxVO.from(Entity)` 静态方法（见 `UserVO.java`）

---

## 错误处理规范

1. 业务异常抛 `BusinessException(ResultCode.XXX)` 或 `BusinessException(ResultCode.XXX, "自定义消息")`
2. `GlobalExceptionHandler` 统一捕获，返回 `Result<Void>`
3. 新业务模块需要的错误码在 `ResultCode.java` 中注册（建议从 2001 开始，避免与 1xxx 用户相关码冲突）
4. 限流触发时返回 HTTP 429，错误码 `TOO_MANY_REQUESTS`，并附带 `Retry-After` 响应头

---

## 可观测性

### 请求链路追踪（RequestIdFilter）

每个入站请求自动生成或透传 `X-Request-Id`，写入 MDC 后注入日志，响应头中同步返回：

```
前端 client.ts  →  X-Request-Id: <uuid>  →  RequestIdFilter → MDC.put("requestId")
                                                              → 响应头 X-Request-Id
                                                              → logback pattern: [%X{requestId}]
```

前端 `client.ts` 的请求拦截器会在每次请求自动注入该头，便于前后端日志联动排查。

### 速率限制（RateLimitFilter）

基于 [Bucket4j](https://bucket4j.com/) 令牌桶算法，对认证接口进行 IP 级限流：

| 接口 | 限额 | 超限响应 |
|------|------|----------|
| `POST /api/auth/login` | 5 次 / 分钟 / IP | HTTP 429 + Retry-After |
| `POST /api/auth/register` | 3 次 / 分钟 / IP | HTTP 429 + Retry-After |

> ⚠️ 当前使用 JVM 内存存储，仅适合单实例。多实例场景需替换为 Bucket4j + Redis。

### API 审计日志（RequestLogAspect）

拦截所有 `@RestController` 方法，自动记录请求摘要：

```
[REQ]  POST /api/auth/login user=anonymous
[RESP] POST /api/auth/login user=anonymous cost=42ms
```

不记录请求参数，避免密码等敏感字段写入日志。

---

## 测试策略

| 层级 | 注解 | 示例文件 |
|------|------|----------|
| 单元测试（Service/工具类） | `@ExtendWith(MockitoExtension.class)` | `UserServiceTest.java` |
| 切片测试（Controller） | `@WebMvcTest` + `@Import(SecurityConfig.class)` | `UserControllerTest.java` |
| E2E 测试（预留） | `@SpringBootTest` + H2 | 目录已创建，待扩展 |

测试用 Token 通过 `TestJwtHelper` 生成，使用与测试配置相同的 secret，可被真实 JwtTokenProvider 校验。

---

## 新增业务模块 Checklist

```
1. [ ] 在 com.music163.starter.<name>/ 下创建模块（直接放根包，不套 module/）
2. [ ] Entity 添加 @Data @Builder @NoArgsConstructor @AllArgsConstructor
3. [ ] 请求入参定义为 XxxRequest，添加 @NotNull/@Size 等校验注解
4. [ ] 响应出参定义为 XxxVO，实现 XxxVO.from(Entity) 静态方法
5. [ ] Service 接口继承 IService<Entity>
6. [ ] Controller 统一返回 Result<T>，添加 @Tag 和 @Operation
7. [ ] 业务异常抛 BusinessException，在 ResultCode 注册错误码
8. [ ] 为 Service 核心方法编写单元测试
```
