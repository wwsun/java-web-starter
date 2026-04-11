# AGENTS.md — AI Agent 行为指令

## 项目概述

这是一个前后端分离的 Web 工程脚手架（java-web-starter），用于快速搭建企业内部管理系统。

子栈详细规范：
- 后端：`backend/AGENTS.md`
- 前端：`frontend/AGENTS.md`

## 项目结构

```
backend/     → Spring Boot 3.2 + JDK 21 后端
frontend/    → Vite + React 19 + TypeScript 前端
doc/         → 项目文档
nginx/       → Nginx 反向代理配置
```

## 禁止事项

- ❌ 不要修改 `common/` 下的基础组件（除非明确需要）
- ❌ 不要绕过 Spring Security 直接处理认证
- ❌ 不要在代码中存储密钥或密码
- ❌ 不要在前端硬编码后端 URL
- ❌ 不要使用 `System.out.println`，使用 SLF4J Logger

## 基础设施命令

```bash
# 启动数据库和缓存（开发必须）
docker compose up -d mysql redis

# 启动所有服务（生产部署）
docker compose up -d
```

## 关键约定

- 遵循 Conventional Commits 提交规范
- API 前缀：`/api`（server.servlet.context-path）
- 数据库：`starter_db`
- 默认管理员：admin / admin123
