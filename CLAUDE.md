# CLAUDE.md — Claude Code 项目上下文

## 项目上下文

这是 `java-web-starter`，一个前后端分离的 Web 工程脚手架。

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

## 编码约定

1. 后端使用 Lombok 注解减少样板代码
2. 所有 Controller 返回 `Result<T>` 统一响应
3. 业务异常使用 `BusinessException`
4. 前端 API 请求通过 `src/api/client.ts`
5. 前端组件使用 TailwindCSS
6. 新增页面需在 `src/router/index.tsx` 注册路由
7. 遵循 Conventional Commits 提交规范

## 包结构

```
com.music163.starter
├── common.result     → Result<T>, ResultCode
├── common.exception  → BusinessException, GlobalExceptionHandler
├── common.config     → MyBatisPlusConfig, RedisConfig, Knife4jConfig
├── security          → SecurityConfig, JWT 认证
├── auth.controller   → AuthController (登录/注册/刷新)
└── module.user       → 用户模块 (entity/mapper/service/controller)
```

## 注意

- Java 版本：21（通过 `.sdkmanrc` 管理）
- Spring Boot 版本：3.2.10
- 前端使用 TailwindCSS v4（@import "tailwindcss" 语法）
- API 前缀：`/api`（server.servlet.context-path）
- 数据库：`starter_db`
- 默认管理员：admin / admin123
