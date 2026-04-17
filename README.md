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

## 本地开发 (VS Code)

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

## 本地开发 (IntelliJ IDEA)

**前置条件**：IntelliJ IDEA 2023+、JDK 21、Node.js 20+、Docker 24+

**Step 0：安装 JDK 21**

推荐通过 sdkman 安装（已安装跳过）：

```bash
cd backend && sdk env install
```

或手动下载 JDK 21，在 IDEA 中配置（File > Project Structure > SDKs）。

**Step 1：导入项目**

打开 IDEA，选择 **File > Open**，选择项目根目录下的 `backend/` 文件夹，IDEA 会自动识别 Maven 工程并下载依赖。

**Step 2：配置 JDK 与注解处理**

- **File > Project Structure > Project**：SDK 设为 JDK 21，Language level 设为 21
- **Settings > Build > Compiler > Annotation Processors**：勾选 **Enable annotation processing**（Lombok 必须）

**Step 3：启动数据库**

```bash
make dev-db
```

**Step 4：运行后端**

找到 `StarterApplication`（`backend/src/main/java/com/music163/starter/StarterApplication.java`），右键 > **Run**。

或创建 Run/Debug Configuration：

- Main class：`com.music163.starter.StarterApplication`
- Active profiles（Spring Boot 选项卡）：`dev`

启动后访问 API 文档：http://localhost:8080/doc.html

**Step 5：启动前端**（IDEA 内置终端或新终端）

```bash
make frontend
# 浏览器访问 http://localhost:5173
# 默认账号：admin / admin123
```

> 本地开发无需启动 Redis（dev profile 使用内存缓存）。

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
