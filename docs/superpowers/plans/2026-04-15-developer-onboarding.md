# Developer Onboarding 改进实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 8 个阻碍国内开发者快速上手的问题，使任意开发者 clone 后按 README 即可顺利运行。

**Architecture:** 全为配置/基础设施层改动，不涉及业务代码。分 5 个独立任务依次执行，每个任务改动集中、可独立验证。README 改动在最后统一完成，避免中间状态误导。

**Tech Stack:** Docker, Maven, npm, Spring Boot (YAML config), Make

---

## 文件变更总览

| 文件 | 操作 | 任务 |
|---|---|---|
| `frontend/Dockerfile` | 修改 | Task 1 |
| `frontend/.npmrc` | 新增 | Task 1 |
| `.mvn/settings.xml` | 新增 | Task 2 |
| `backend/src/main/resources/application-dev.yml` | 修改 | Task 3 |
| `docker-compose.yml` | 修改 | Task 3 |
| `Makefile` | 新增 | Task 4 |
| `.env.example` | 修改 | Task 5 |
| `frontend/.env.example` | 新增 | Task 5 |
| `README.md` | 修改 | Task 5 |

---

## Task 1：网络加速——Docker 镜像 + npm 镜像（A1、A3）

**Files:**
- Modify: `frontend/Dockerfile`
- Create: `frontend/.npmrc`

- [ ] **Step 1：修复前端 Dockerfile 第二阶段镜像**

打开 `frontend/Dockerfile`，将第 6 行（第二阶段 FROM）从：
```dockerfile
FROM nginx:alpine
```
改为：
```dockerfile
FROM docker.m.daocloud.io/library/nginx:alpine
```

完整文件内容：
```dockerfile
# ==========================================
# Stage 1: Build
# ==========================================
FROM docker.m.daocloud.io/library/node:20-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ==========================================
# Stage 2: Serve with Nginx
# ==========================================
FROM docker.m.daocloud.io/library/nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config for SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

- [ ] **Step 2：新增 frontend/.npmrc**

新建 `frontend/.npmrc`，内容：
```
registry=https://registry.npmmirror.com
```

- [ ] **Step 3：验证 Dockerfile 语法正确**

```bash
docker build --no-cache -f frontend/Dockerfile frontend/ --target build -t test-frontend-build 2>&1 | tail -5
```

预期：最后一行包含 `Successfully built` 或 `CACHED`，无 `FROM` 相关错误。如果不想等全量构建，验证 FROM 行正确即可：
```bash
grep "^FROM" frontend/Dockerfile
```
预期输出：
```
FROM docker.m.daocloud.io/library/node:20-alpine AS build
FROM docker.m.daocloud.io/library/nginx:alpine
```

- [ ] **Step 4：提交**

```bash
git add frontend/Dockerfile frontend/.npmrc
git commit -m "chore: use DaoCloud mirror for frontend nginx image and add npmrc"
```

---

## Task 2：网络加速——Maven 国内镜像（A2）

**Files:**
- Create: `.mvn/settings.xml`

- [ ] **Step 1：创建 .mvn 目录并新增 settings.xml**

```bash
mkdir -p .mvn
```

新建 `.mvn/settings.xml`，内容：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

- [ ] **Step 2：验证 Maven 能读取该配置**

```bash
cd backend && mvn help:effective-settings -s ../.mvn/settings.xml 2>&1 | grep -A3 "aliyun"
```

预期：输出包含 `aliyun` 和阿里云 URL，说明镜像配置生效。

- [ ] **Step 3：提交**

```bash
git add .mvn/settings.xml
git commit -m "chore: add .mvn/settings.xml with aliyun mirror for domestic developers"
```

---

## Task 3：Redis 可选化（B1）

**Files:**
- Modify: `backend/src/main/resources/application-dev.yml`
- Modify: `docker-compose.yml`

- [ ] **Step 1：修改 application-dev.yml，改用内存缓存**

将 `backend/src/main/resources/application-dev.yml` 中的 Cache 配置块：
```yaml
  # Cache
  cache:
    type: redis
    redis:
      time-to-live: 3600000
      key-prefix: "starter:"
      use-key-prefix: true
```
替换为：
```yaml
  # Cache（dev 环境使用内存缓存，无需启动 Redis）
  cache:
    type: simple
```

- [ ] **Step 2：验证后端在无 Redis 的情况下能启动**

确保 MySQL 在运行，Redis 未运行：
```bash
docker compose up -d mysql
docker compose stop redis 2>/dev/null || true
```

启动后端：
```bash
cd backend && mvn spring-boot:run -s ../.mvn/settings.xml -q 2>&1 | grep -E "(Started|ERROR|redis)" | head -10
```

预期：出现 `Started` 日志，无 `redis connection refused` 类错误。确认后 Ctrl+C 停止。

- [ ] **Step 3：修改 docker-compose.yml，移除 backend 对 redis 的 depends_on**

将 `docker-compose.yml` 中 `backend` 服务的 `depends_on` 从：
```yaml
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
```
改为：
```yaml
    depends_on:
      mysql:
        condition: service_healthy
```

- [ ] **Step 4：提交**

```bash
git add backend/src/main/resources/application-dev.yml docker-compose.yml
git commit -m "fix: make Redis optional in dev profile, remove compose depends_on"
```

---

## Task 4：统一启动入口——Makefile（E1）

**Files:**
- Create: `Makefile`

- [ ] **Step 1：新建 Makefile**

在项目根目录新建 `Makefile`（注意：缩进必须使用 **Tab**，不能用空格）：

```makefile
.PHONY: dev-db backend frontend up down test logs help

MVN_OPTS = -s .mvn/settings.xml

help: ## 显示所有可用命令
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

dev-db: ## 启动本地开发数据库（MySQL）
	docker compose up -d mysql

backend: ## 启动后端开发服务器（需先运行 dev-db）
	cd backend && sdk env && mvn spring-boot:run $(MVN_OPTS)

frontend: ## 启动前端开发服务器
	cd frontend && npm install && npm run dev

up: ## 全栈 Docker 启动（生产模式，含 Redis）
	docker compose up -d --build

down: ## 停止所有 Docker 服务
	docker compose down

test: ## 运行前后端测试
	cd backend && mvn test $(MVN_OPTS)
	cd frontend && npm run test:run

logs: ## 查看后端容器日志
	docker compose logs -f backend
```

- [ ] **Step 2：验证 make help 输出正常**

```bash
make help
```

预期输出（颜色高亮，列对齐）：
```
  dev-db       启动本地开发数据库（MySQL）
  backend      启动后端开发服务器（需先运行 dev-db）
  frontend     启动前端开发服务器
  up           全栈 Docker 启动（生产模式，含 Redis）
  down         停止所有 Docker 服务
  test         运行前后端测试
  logs         查看后端容器日志
```

- [ ] **Step 3：提交**

```bash
git add Makefile
git commit -m "chore: add Makefile with common dev commands"
```

---

## Task 5：文档与配置引导更新（B2/B3、C1、C2、D1、D2）

**Files:**
- Modify: `README.md`
- Modify: `.env.example`
- Create: `frontend/.env.example`

- [ ] **Step 1：在 .env.example 末尾追加 CORS 变量**

在 `.env.example` 末尾追加：
```bash

# ----------------------------------------
# CORS 允许来源
# 本地开发默认 localhost:5173；部署时改为实际域名
# ----------------------------------------
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

- [ ] **Step 2：新建 frontend/.env.example**

新建 `frontend/.env.example`，内容：
```bash
# 启用 MSW Mock 数据（无需后端时设为 true）
VITE_ENABLE_MOCK=false
```

- [ ] **Step 3：更新 README.md**

将 `README.md` 全文替换为以下内容：

```markdown
# java-web-starter

企业内部管理系统前后端分离脚手架。内置用户管理、角色权限、JWT 双令牌认证，`fork` 后即可快速启动新项目。

> **使用前必读**：本仓库为脚手架模板，后端包名默认为 `com.music163.starter`。
> fork 新项目后，请先按 [TODO.md](TODO.md) 第 0 步完成包名替换，再开始开发。

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

> 首次构建需下载依赖，耗时约 **10~20 分钟**；后续重启约 1 分钟。
> 可用 `docker compose logs -f backend` 实时观察启动进度，看到 `Started Application` 即表示就绪。

启动后访问 **http://localhost:8090**（默认账号：`admin / admin123`）。

> 查看启动状态：`docker compose ps` · 查看后端日志：`docker compose logs -f backend`

## 本地开发

**前置条件**：[sdkman](https://sdkman.io)、Node.js 20+、Docker 24+

**Step 0：安装 sdkman 并配置 Java + Maven 版本**

```bash
# 安装 sdkman（已安装跳过）
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 在 backend/ 目录下自动安装并切换到项目指定版本（Java 21 + Maven 3.9）
cd backend && sdk env install
```

**Step 1：启动数据库**

```bash
make dev-db
```

**Step 2：启动后端**（新终端）

```bash
make backend
# API 文档：http://localhost:8080/doc.html
```

**Step 3：启动前端**（新终端）

```bash
make frontend
# 浏览器访问 http://localhost:5173
# 默认账号：admin / admin123
```

> 本地开发无需启动 Redis（dev profile 使用内存缓存）。
> 生产部署（`docker compose up`）自动启用 Redis。

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
cp .env.example .env
# 编辑 .env，填入真实密码和密钥
```

**Step 2：构建并启动**

```bash
make up
```

**Step 3：验证**

```bash
docker compose ps
# 浏览器访问 http://your-server-ip:8090
```

常用维护命令：

```bash
make logs                                          # 查看后端日志
docker compose pull && docker compose up -d --build   # 更新部署
make down                                          # 停止服务
docker compose down -v                             # 停止并清除持久化数据（慎用）
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
Makefile     → 常用开发命令入口（make help 查看）
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
make test
```
```

- [ ] **Step 4：验证 README 渲染无格式破损**

```bash
# 检查 markdown 中无明显语法错误（未闭合代码块等）
grep -c '```' README.md
```

预期：输出为偶数（代码块成对闭合）。

- [ ] **Step 5：提交**

```bash
git add README.md .env.example frontend/.env.example
git commit -m "docs: update README for domestic devs - sdkman, Redis optional, timing, make commands"
```

---

## 验收检查

所有任务完成后，按以下顺序做端到端验证：

```bash
# 1. 检查 Dockerfile FROM 行一致性
grep "^FROM" frontend/Dockerfile
# 期望：两行都含 docker.m.daocloud.io

# 2. 检查 Maven 镜像配置存在
cat .mvn/settings.xml | grep aliyun
# 期望：输出含 aliyun

# 3. 检查 npm 镜像配置存在
cat frontend/.npmrc
# 期望：registry=https://registry.npmmirror.com

# 4. 检查 dev.yml cache 类型
grep "type:" backend/src/main/resources/application-dev.yml
# 期望：type: simple

# 5. 检查 compose 中 backend 不依赖 redis
grep -A5 "depends_on" docker-compose.yml
# 期望：仅含 mysql，无 redis

# 6. 验证 make help 正常
make help

# 7. 检查 README 中包名警告存在
grep "使用前必读" README.md
```
