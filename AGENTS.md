# AGENTS.md — AI Agent 行为指令

## 项目概述

这是一个前后端分离的 Web 工程脚手架（java-web-starter），用于快速搭建企业内部管理系统。

## 技术栈约束

### 后端

- **语言**: Java 21
- **框架**: Spring Boot 3.2.x
- **ORM**: MyBatis-Plus 3.5.x（禁止使用 JPA/Hibernate）
- **数据库**: MySQL 8.4
- **缓存**: Redis 7 + Spring Cache
- **认证**: Spring Security + JWT（JJWT 库）
- **API 文档**: Knife4j（SpringDoc OpenAPI）
- **构建工具**: Maven

### 前端

- **框架**: React 19 + TypeScript（严格模式）
- **构建**: Vite
- **样式**: TailwindCSS v4
- **路由**: React Router v6
- **HTTP**: Axios（封装在 src/api/client.ts）
- **状态管理**: Zustand

## 代码规范

> [!IMPORTANT]
> **规范优先级**：当《Alibaba Java Coding Guidelines》与本项目特定的规范文档（如 `doc/dev-guide.md`、`doc/api-convention.md`）存在冲突时，**优先遵循本项目特定规范**。

### 后端

1. **所有实体类使用 Lombok 注解**：`@Data`, `@Builder`, `@RequiredArgsConstructor` 等
2. **Controller 层**：
   - 统一返回 `Result<T>` 包装类
   - 使用 `@Validated` 进行参数校验
   - 添加 `@Tag` 和 `@Operation` Knife4j 注解
3. **Service 层**：
   - 接口 + 实现类模式（`UserService` + `UserServiceImpl`）
   - 继承 `IService<Entity>`
4. **异常处理**：
   - 业务异常抛 `BusinessException`
   - 禁止在 Controller 层 try-catch
5. **数据库**：
   - 表名和字段名使用 snake_case
   - 逻辑删除字段统一为 `deleted`
   - 时间字段使用 `created_at` / `updated_at`
6. **通用编码规范**：参考 [Alibaba Java Coding Guidelines](doc/java-coding-guidelines.md)

### 前端

1. **TypeScript 严格模式**：禁止使用 `any`
2. **组件命名**: PascalCase（`MeetingRoomList.tsx`）
3. **API 请求**：统一通过 `src/api/client.ts` 发起
4. **状态管理**：使用 Zustand store，文件放在 `src/stores/`
5. **样式**：使用 TailwindCSS utility classes

## 新增业务模块模板

### 后端模块

在 `backend/src/main/java/com/music163/starter/module/` 下创建：

```
module/<name>/
├── controller/<Name>Controller.java
├── service/<Name>Service.java
├── service/impl/<Name>ServiceImpl.java
├── mapper/<Name>Mapper.java
└── entity/<Name>.java
```

### 前端页面

在 `frontend/src/pages/` 下创建页面，并在 `src/router/index.tsx` 中注册路由。

## 禁止事项

- ❌ 不要修改 `common/` 下的基础组件（除非明确需要）
- ❌ 不要在前端硬编码后端 URL
- ❌ 不要绕过 Spring Security 直接处理认证
- ❌ 不要在代码中存储密钥或密码
- ❌ 不要使用 `System.out.println`，使用 SLF4J Logger

## 项目结构

```
backend/     → Spring Boot 3.2 + JDK 21 后端
frontend/    → Vite + React 19 + TypeScript 前端
doc/         → 项目文档
nginx/       → Nginx 反向代理配置
```

## 关键文件

- `backend/pom.xml` — Maven 依赖管理
- `backend/src/main/resources/application-dev.yml` — 开发环境配置
- `frontend/vite.config.ts` — Vite 配置（API 代理在此）
- `frontend/src/api/client.ts` — Axios 实例（Token 注入、错误处理）
- `frontend/src/router/index.tsx` — 路由配置
- `docker-compose.yml` — 基础设施编排

## 开发命令

```bash
# 后端编译
cd backend && ./mvnw clean compile

# 后端启动
cd backend && ./mvnw spring-boot:run

# 前端安装依赖
cd frontend && npm install

# 前端启动
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build

# 启动基础设施
docker compose up -d mysql redis
```

## 后端包结构

```
com.music163.starter
├── common.result     → Result<T>, ResultCode
├── common.exception  → BusinessException, GlobalExceptionHandler
├── common.config     → MyBatisPlusConfig, RedisConfig, Knife4jConfig
├── security          → SecurityConfig, JWT 认证
├── auth.controller   → AuthController (登录/注册/刷新)
└── module.user       → 用户模块 (entity/mapper/service/controller)
```

## 关键约定

- Java 版本：21（通过 `.sdkmanrc` 管理）
- Spring Boot 版本：3.2.10
- 前端使用 TailwindCSS v4（`@import "tailwindcss"` 语法）
- API 前缀：`/api`（server.servlet.context-path）
- 数据库：`starter_db`
- 默认管理员：admin / admin123
- 遵循 Conventional Commits 提交规范
