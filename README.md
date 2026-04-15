# java-web-starter

企业内部管理系统前后端分离脚手架。内置用户管理、角色权限、JWT 双令牌认证，`fork` 后即可快速启动新项目。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.2 · MyBatis-Plus · Spring Security · JJWT |
| 前端 | React 19 · TypeScript · Vite · TailwindCSS v4 · shadcn/ui · Zustand |
| 基础设施 | MySQL 8.4 · Redis 7 · Nginx · Docker Compose |

## 快速验证

**前置条件**：Docker 24+

无需本地安装 JDK 或 Node.js，一条命令启动所有服务：

```bash
docker compose up -d --build
```

约 2 分钟后访问 **http://localhost:8090**（默认账号：`admin / admin123`）。

> 查看启动状态：`docker compose ps` · 查看后端日志：`docker compose logs -f backend`

## 本地开发

**前置条件**：JDK 21、Node.js 20+、Docker

**Step 1：启动数据库**

```bash
docker compose up -d mysql
```

**Step 2：启动后端**

```bash
cd backend
mvn spring-boot:run
# API 文档：http://localhost:8080/doc.html
```

**Step 3：启动前端**

```bash
cd frontend
npm install
npm run dev
# 浏览器访问 http://localhost:5173
# 默认账号：admin / admin123
```

> 纯前端开发（不依赖后端）：`VITE_ENABLE_MOCK=true npm run dev`

**API 调试**

```bash
source scripts/api.sh     # 加载工具（每个终端会话执行一次）
api_health                # 检查后端是否运行
api_get /api/users/me     # token 自动缓存，首次调用自动登录
```

## 部署

**前置条件**：Docker 24+、Docker Compose v2

**Step 1：配置生产环境变量**

```bash
# 创建 .env 文件（覆盖 docker-compose.yml 中的默认值）
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=your-strong-password
JWT_SECRET=your-256-bit-secret-key-change-in-production
EOF
```

**Step 2：构建并启动**

```bash
docker compose up -d --build
```

**Step 3：验证**

```bash
docker compose ps
# 浏览器访问 http://your-server-ip:8090（如已用 80 端口改为不冲突的端口）
```

常用维护命令：

```bash
docker compose logs -f backend    # 查看后端日志
docker compose pull && docker compose up -d --build   # 更新部署
docker compose down               # 停止服务
docker compose down -v            # 停止并清除持久化数据（慎用）
```

详细环境变量说明、CI/CD 模板及数据库管理见 [部署指南](doc/deploy-guide.md)。

## 项目结构

```
backend/     → Spring Boot 后端（用户/角色模块为示范）
frontend/    → React 前端（Login / Dashboard / 用户列表为示范）
doc/         → 架构设计、API 规范、开发指南
scripts/     → 开发辅助脚本（api.sh：API 调试与回归测试）
nginx/       → Nginx 反向代理配置
docker-compose.yml
TODO.md      → 基于本脚手架新建项目的初始化清单
```

## 文档导航

- [架构说明](doc/architecture.md) — 分层约束、双令牌认证机制、可观测性设计
- [API 规范](doc/api-convention.md) — URL 命名、HTTP Method、统一响应格式
- [如何新增模块](doc/how-to-add-module.md) — 7 步从 Spec 到上线的完整流程
- [开发规范](doc/dev-guide.md) — Git 分支策略、提交信息规范、代码审查 Checklist
- [部署指南](doc/deploy-guide.md) — Docker Compose 部署、环境变量、CI/CD 模板

## 运行测试

```bash
# 后端
cd backend && mvn test

# 前端
cd frontend && npm run test:run
```
