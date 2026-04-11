# backend/AGENTS.md — 后端开发规范

## 技术栈

- **语言**: Java 21
- **框架**: Spring Boot 3.2.x
- **ORM**: MyBatis-Plus 3.5.x（禁止使用 JPA/Hibernate）
- **数据库**: MySQL 8.4
- **缓存**: Redis 7 + Spring Cache
- **认证**: Spring Security + JWT（JJWT 库）
- **API 文档**: Knife4j（SpringDoc OpenAPI）
- **构建工具**: Maven

## 代码规范

> 规范优先级：当《Alibaba Java Coding Guidelines》与本项目特定规范（`doc/dev-guide.md`、`doc/api-convention.md`）冲突时，**优先遵循本项目规范**。

1. **所有实体类使用 Lombok 注解**：`@Data`, `@Builder`, `@RequiredArgsConstructor` 等
2. **Controller 层**：
   - 统一返回 `Result<T>` 包装类
   - 使用 `@Validated` 进行参数校验
   - 添加 `@Tag` 和 `@Operation` Knife4j 注解
3. **Service 层**：
   - 接口 + 实现类模式（`UserService` + `UserServiceImpl`）
   - 继承 `IService<Entity>`
   - 实现类与接口放在同级，不套 `impl/` 子包
4. **异常处理**：
   - 业务异常抛 `BusinessException`
   - 禁止在 Controller 层 try-catch
5. **数据库**：
   - 表名和字段名使用 snake_case
   - 逻辑删除字段统一为 `deleted`
   - 时间字段使用 `created_at` / `updated_at`
6. **通用编码规范**：参考 [Alibaba Java Coding Guidelines](../doc/java-coding-guidelines.md)

## DTO / VO 规范

- 请求入参使用 `XxxRequest`，放在 `<name>/dto/`，添加 `@NotNull`/`@Size` 等校验注解
- 响应出参使用 `XxxVO`，放在 `<name>/`，不包含 `password`、`deleted` 等敏感字段
- Entity 不得直接作为 Controller 返回值，也直接放在 `<name>/`
- VO 转换使用 `XxxVO.from(Entity)` 静态工厂方法，放在 VO 类中

## 获取当前登录用户

```java
String username = SecurityContextHolder.getContext().getAuthentication().getName();
```

不要在 Controller 方法参数中手动解析 `Authorization` Header。

## 测试规范

- Service 层业务逻辑必须有对应单元测试（`@ExtendWith(MockitoExtension.class)`，不依赖 Spring 容器）
- Controller 层使用 `@WebMvcTest` + `@Import(SecurityConfig.class)` 做切片测试
- 测试用 Token 统一通过 `TestJwtHelper` 生成，不在测试中硬编码 JWT 字符串
- 测试类命名：`<被测类名>Test.java`

## 新增业务模块模板

业务模块直接位于 `com.music163.starter` 下（不套 `module/` 中间层）：

```
<name>/
├── <Name>Controller.java
├── <Name>Service.java
├── <Name>ServiceImpl.java   # 与接口同级
├── <Name>Mapper.java
├── <Name>.java             # Entity
├── <Name>VO.java           # 响应出参
└── dto/
    └── <Name>Request.java  # 请求入参
```

## 包结构

```
com.music163.starter
├── auth              → 认证模块 (AuthController, dto/)
├── user              → 用户模块
├── role              → 角色模块
├── security          → Spring Security 核心配置
├── common.result     → Result<T>, ResultCode
├── common.exception  → BusinessException, GlobalExceptionHandler
└── common.config     → MyBatisPlusConfig, RedisConfig, Knife4jConfig
```

## 关键文件

- `pom.xml` — Maven 依赖管理
- `src/main/resources/application-dev.yml` — 开发环境配置

## 开发命令

```bash
# 编译
mvn clean compile

# 启动
mvn spring-boot:run

# 运行全部测试
mvn test
```

## 关键约定

- Java 版本：21（通过 `.sdkmanrc` 管理）
- Spring Boot 版本：3.2.10
