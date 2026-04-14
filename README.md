# java-web-starter

企业内部管理系统前后端分离脚手架。内置用户管理、角色权限、JWT 双令牌认证，`fork` 后即可快速启动新项目。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.2 · MyBatis-Plus · Spring Security · JJWT |
| 前端 | React 19 · TypeScript · Vite · TailwindCSS v4 · shadcn/ui · Zustand |
| 基础设施 | MySQL 8.4 · Redis 7 · Nginx · Docker Compose |

## 快速启动

**前置条件**：JDK 21、Node.js 20+、Docker

**Step 1：启动数据库**

```bash
docker compose up -d mysql
```

**Step 2：启动后端**

```bash
cd backend
mvn spring-boot:run
# 后端启动后访问 API 文档：http://localhost:8080/doc.html
```

**Step 3：启动前端**

```bash
cd frontend
npm install
npm run dev
# 浏览器访问 http://localhost:5173
# 默认账号：admin / admin123
```

> 前端独立开发（无需后端）：`VITE_ENABLE_MOCK=true npm run dev`

## 项目结构

```
backend/     → Spring Boot 后端（用户/角色模块为示范）
frontend/    → React 前端（Login / Dashboard / 用户列表为示范）
doc/         → 架构设计、API 规范、开发指南
nginx/       → Nginx 反向代理配置
docker-compose.yml
```

## 文档导航

- [架构说明](doc/architecture.md) — 分层约束、双令牌认证机制、可观测性设计
- [API 规范](doc/api-convention.md) — URL 命名、HTTP Method、统一响应格式
- [如何新增模块](doc/how-to-add-module.md) — 7 步从 Spec 到上线的完整流程
- [开发规范](doc/dev-guide.md) — Git 分支策略、提交信息规范、代码审查 Checklist

## 运行测试

```bash
# 后端
cd backend && mvn test

# 前端
cd frontend && npm run test:run
```
