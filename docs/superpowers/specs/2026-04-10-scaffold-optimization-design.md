# 脚手架优化设计文档

**日期**：2026-04-10  
**状态**：已批准  
**范围**：java-web-starter 脚手架自身完善，不涉及具体业务逻辑实现

---

## 背景与目标

### 背景

java-web-starter 是一套面向企业内部管理系统的前后端分离脚手架，供团队通过 fork/clone 快速启动新项目。当前脚手架已具备认证、用户管理、基础配置等能力，但存在若干安全漏洞、架构规范违反和工程化缺失问题，影响其作为高质量起点的可信度。

### 目标

打磨脚手架本身，使其在以下维度达到高标准：

1. **安全**：修复已知安全漏洞，建立正确的认证模型
2. **架构规范**：建立 DTO/VO 模式作为完整示例，修正分层违反
3. **可测试性**：提供三层测试示例和测试基础设施
4. **文档**：完善 AGENTS.md 规范 + 新增架构说明文档

---

## 一、安全漏洞修复

### 1.1 Token 类型隔离

**问题**：`JwtTokenProvider` 生成 token 时写入了 `type` claim（`"access"` / `"refresh"`），但过滤器和 refresh 接口均未校验此字段，导致：
- refresh token 可直接调用业务 API
- access token 可调用 `/auth/refresh` 接口完成刷新

**修复方案**：

在 `JwtTokenProvider` 新增两个校验方法：

```java
// 校验签名 + 未过期 + type == "access"
public boolean validateAccessToken(String token)

// 校验签名 + 未过期 + type == "refresh"  
public boolean validateRefreshToken(String token)
```

- `JwtAuthenticationFilter` 改为调用 `validateAccessToken()`
- `AuthController.refresh()` 改为调用 `validateRefreshToken()`
- 原 `validateToken()` 保留为私有内部方法复用

### 1.2 密码字段隔离

**问题**：`UserController` 的列表和详情接口直接返回 `User` 实体，`password` 字段（BCrypt 哈希）随响应泄露。

**修复方案**：

新增 `UserVO`，仅包含对外安全的字段：

```java
// 包含字段：id, username, nickname, email, phone, avatar, status, createdAt
// 排除字段：password, deleted, updatedAt
public class UserVO { ... }
```

转换逻辑统一使用 `UserVO.from(User)` 静态工厂方法，放在 `UserVO` 类中，Controller 和 Service 均调用此方法，保持职责单一。

### 1.3 AuthController 分层修复

**问题**：`AuthController` 直接注入 `UserMapper`，`register()` 方法在 Controller 层手动构造 `User` 对象并调用 `userMapper.insert()`，业务逻辑散落在 Controller，违反分层规范。

**修复方案**：

将注册逻辑移入 `UserService.register(RegisterRequest)`，`AuthController` 只负责：
1. 调用 `AuthenticationManager` 完成认证
2. 调用 `UserService.register()` 完成注册
3. 生成并返回 Token

`AuthController` 依赖从 `UserMapper` 改为 `UserService`。

---

## 二、架构规范完善

### 2.1 DTO/VO 模式

User 模块作为完整的 DTO/VO 示范，供 fork 者参考复制。

**命名约定**：

| 类型 | 用途 | 存放位置 |
|------|------|----------|
| `XxxRequest` | 请求入参，含 `@Valid` 校验注解 | `module/<name>/dto/` |
| `XxxVO` | 响应出参，安全字段子集 | `module/<name>/vo/` |
| `Entity` | 数据库映射，不得出现在 Controller 返回值中 | `module/<name>/entity/` |

**User 模块新增文件**：

```
module/user/
├── dto/
│   ├── UpdateUserRequest.java      # 更新用户信息（nickname/email/phone/avatar）
│   └── ChangePasswordRequest.java  # 修改密码（oldPassword/newPassword）
├── vo/
│   └── UserVO.java                 # 对外响应（不含 password/deleted）
```

### 2.2 UserController 接口完善

| 方法 | 路径 | 变化 |
|------|------|------|
| GET | `/users` | 返回类型改为 `Result<IPage<UserVO>>` |
| GET | `/users/{id}` | 返回类型改为 `Result<UserVO>` |
| GET | `/users/me` | **新增**：获取当前登录用户信息 |
| PUT | `/users/me` | **新增**：更新当前用户信息 |
| PUT | `/users/me/password` | **新增**：修改密码 |
| DELETE | `/users/{id}` | 增加防自删保护 |

`/users/me` 系列接口是脚手架中"获取当前登录用户"的标准示例，从 `SecurityContextHolder` 获取用户名，不从请求参数传入。

### 2.3 Entity 规范修复

`User.java` 补充缺失注解（MyBatis-Plus 需要无参构造，Builder 模式需要全参构造）：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User implements Serializable { ... }
```

### 2.4 配置修复

| 问题 | 修复位置 | 修复方式 |
|------|----------|----------|
| `log-impl: StdOutImpl` 全局开启，生产性能问题 | `application.yml` | 移到 `application-dev.yml` |
| `TokenResponse.of(token, refresh, 3600)` 硬编码 | `AuthController.java` | 改为读取 `jwt.access-token-expiration / 1000` |
| `deleteUser` 无防自删保护 | `UserController.java` | 从 SecurityContext 取当前用户 id，相同则抛 `BusinessException` |

---

## 三、测试基础设施

### 3.1 测试分层策略

```
单元测试（Unit）   → 纯逻辑，@ExtendWith(MockitoExtension.class)，无 Spring 容器
切片测试（Slice）  → @WebMvcTest，只加载 Web 层 Bean
端到端测试（E2E）  → @SpringBootTest + H2（预留目录结构，本次不实现具体用例）
```

### 3.2 测试文件结构

```
test/java/com/music163/starter/
├── common/
│   └── TestJwtHelper.java           # 生成测试用 token，供所有测试复用
├── security/
│   └── JwtTokenProviderTest.java    # 单元测试（6 个 case）
├── module/user/
│   ├── UserServiceTest.java         # 单元测试（5 个 case）
│   └── UserControllerTest.java      # 切片测试（4 个 case）
└── auth/
    └── AuthControllerTest.java      # 切片测试（4 个 case）
```

### 3.3 JwtTokenProviderTest 用例

```
✓ generateAccessToken 生成的 token type 为 "access"
✓ generateRefreshToken 生成的 token type 为 "refresh"
✓ validateAccessToken 拒绝 refresh token
✓ validateRefreshToken 拒绝 access token
✓ validateAccessToken 拒绝过期 token
✓ getUsernameFromToken 正确提取用户名
```

### 3.4 UserServiceTest 用例

```
✓ register 成功创建用户（密码经 BCrypt 编码）
✓ register 用户名已存在时抛 BusinessException
✓ findByUsername 用户不存在返回 null
✓ toVO 返回的 UserVO 不含 password 字段
✓ changePassword 旧密码错误时抛 BusinessException
```

### 3.5 UserControllerTest 用例

```
✓ GET /users/me 未认证时返回 401
✓ GET /users/me 认证后返回 UserVO（响应中不含 password 字段）
✓ DELETE /users/{id} 删除自己时返回 400
✓ PUT /users/me/password 旧密码错误时返回 400
```

### 3.6 AuthControllerTest 用例

```
✓ POST /auth/login 成功返回 access_token + refresh_token
✓ POST /auth/login 密码错误返回 401
✓ POST /auth/register 用户名重复返回 400
✓ POST /auth/refresh 使用 access token 时返回 401
```

### 3.7 测试依赖

`pom.xml` 补充 H2 内嵌数据库（用于 E2E 测试）：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 四、文档完善

### 4.1 AGENTS.md 补充内容

在现有 AGENTS.md 的"代码规范 - 后端"章节补充三块：

**DTO/VO 规范**：
- 请求入参使用 `XxxRequest`，放在 `module/<name>/dto/`
- 响应出参使用 `XxxVO`，放在 `module/<name>/vo/`
- Entity 不得直接作为 Controller 返回值（防止敏感字段泄露）
- VO 转换使用 `XxxVO.from(Entity)` 静态工厂方法，放在 VO 类中

**测试规范**：
- Service 层逻辑必须有对应单元测试（Mockito，不依赖 Spring 容器）
- Controller 层使用 `@WebMvcTest` 做切片测试
- 测试用 Token 统一通过 `TestJwtHelper` 生成，不硬编码字符串
- 测试类命名：`<被测类名>Test.java`

**获取当前登录用户的标准方式**：
```java
String username = SecurityContextHolder.getContext()
    .getAuthentication().getName();
```
不要在 Controller 方法参数中手动解析 Token Header。

### 4.2 新增 doc/architecture.md

结构：

```
# 脚手架架构说明

1. 定位与目标
2. 技术选型决策
   - 为什么用 MyBatis-Plus 而非 JPA
   - 为什么用 JJWT + 双令牌方案
   - 为什么用 Knife4j
3. 分层架构（各层职责边界、禁止的跨层调用）
4. 认证机制（双令牌流程图）
5. DTO/VO 规范（User 模块完整示例）
6. 错误处理规范（ResultCode 扩展方式）
7. 测试策略（三层测试说明 + 示例文件位置）
8. 新增业务模块 Checklist（8 步）
```

**新增业务模块 Checklist（核心内容）**：

```markdown
1. [ ] 在 `module/<name>/` 下创建 entity/dto/vo/mapper/service/controller
2. [ ] Entity 添加 @Builder @NoArgsConstructor @AllArgsConstructor @Data
3. [ ] 请求入参定义为 XxxRequest，添加 @NotNull/@Size 等校验注解
4. [ ] 响应出参定义为 XxxVO，不包含敏感字段
5. [ ] Service 接口继承 IService<Entity>
6. [ ] Controller 统一返回 Result<T>，添加 @Tag 和 @Operation
7. [ ] 业务异常抛 BusinessException，在 ResultCode 中注册错误码
8. [ ] 为 Service 逻辑编写单元测试
```

---

## 变更影响范围

| 文件/目录 | 变更类型 |
|-----------|----------|
| `security/JwtTokenProvider.java` | 修改：新增 validateAccessToken/validateRefreshToken |
| `security/JwtAuthenticationFilter.java` | 修改：调用 validateAccessToken |
| `auth/controller/AuthController.java` | 修改：依赖改为 UserService，修复 refresh 校验 |
| `module/user/dto/` | 新增：UpdateUserRequest, ChangePasswordRequest |
| `module/user/vo/UserVO.java` | 新增 |
| `module/user/entity/User.java` | 修改：补 @Builder/@NoArgsConstructor/@AllArgsConstructor |
| `module/user/service/UserService.java` | 修改：新增 register/toVO/changePassword 方法 |
| `module/user/service/impl/UserServiceImpl.java` | 修改：实现新方法 |
| `module/user/controller/UserController.java` | 修改：返回类型改 VO，新增 /me 接口，加防自删 |
| `src/main/resources/application.yml` | 修改：移除 StdOutImpl |
| `src/main/resources/application-dev.yml` | 修改：加入 StdOutImpl |
| `test/` | 新增：TestJwtHelper + 4 个测试类（19 个用例）|
| `AGENTS.md` | 修改：补充 DTO/VO 规范、测试规范、获取当前用户方式 |
| `doc/architecture.md` | 新增 |

---

## 不在本次范围内

- Flyway 数据库 migration（可选工程化增强，优先级低）
- 前端 token 自动刷新拦截器（前端工程化增强）
- 认证接口限流（Bucket4j，生产增强项）
- Redis 存储 refresh token 以支持主动吊销（高级安全特性）
- 会议室等具体业务模块实现
