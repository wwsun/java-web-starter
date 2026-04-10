# Java Web Starter

> 一套可复用的前后端分离 Web 工程基线，面向企业内部系统开发（如会议室预订），后续项目通过 fork / clone 快速启动。

## 技术栈

| 层级 | 技术 |
|---|---|
| **后端** | Spring Boot 3.2 · JDK 21 · MyBatis-Plus · Spring Security + JWT · Redis · Knife4j |
| **前端** | Vite · React 19 · TypeScript · TailwindCSS v4 · React Router v6 · Axios · Zustand |
| **基础设施** | Docker Compose · MySQL 8.4 · Redis 7 · Nginx |

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
# 克隆项目
git clone <repo-url>
cd java-web-starter

# 一键启动基础设施（MySQL + Redis）
docker compose up -d mysql redis

# 等待服务就绪后启动后端
docker compose up -d backend

# 启动前端
docker compose up -d frontend nginx

# 访问
# 前端: http://localhost
# 后端 API: http://localhost/api
# API 文档: http://localhost:8080/api/doc.html
```

### 方式二：本地开发

#### 前置条件

- JDK 21（推荐使用 [SDKMAN](https://sdkman.io/) 管理）
- Node.js 20+
- Docker（用于 MySQL 和 Redis）

#### 启动步骤

```bash
# 1. 启动数据库和缓存
docker compose up -d mysql redis

# 2. 启动后端
cd backend
./mvnw spring-boot:run

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev

# 4. 访问
# 前端: http://localhost:5173
# 后端 API: http://localhost:8080/api
# API 文档: http://localhost:8080/api/doc.html
```

#### 启用 Mock 数据（可选）

前端支持通过 MSW 进行独立开发，无需启动后端：

```bash
cd frontend
VITE_ENABLE_MOCK=true npm run dev
```

## 项目结构

```
├── doc/                        # 项目文档
│   ├── sql/init.sql            # 数据库初始化脚本
│   ├── api-convention.md       # API 设计规范
│   ├── dev-guide.md            # 开发规范
│   └── deploy-guide.md         # 部署指南
├── backend/                    # Java 后端（Spring Boot）
│   ├── src/main/java/com/music163/starter/
│   │   ├── common/             # 通用组件（Result, 异常处理, 配置）
│   │   ├── security/           # 安全认证（JWT, Spring Security）
│   │   ├── module/             # 业务模块
│   │   └── auth/               # 认证接口
│   └── src/main/resources/     # 配置文件
├── frontend/                   # 前端（Vite + React + TypeScript）
│   └── src/
│       ├── api/                # HTTP 层（Axios）
│       ├── layouts/            # 布局组件
│       ├── pages/              # 页面组件
│       ├── router/             # 路由配置
│       ├── stores/             # 状态管理（Zustand）
│       └── mocks/              # Mock 数据（MSW）
├── nginx/                      # Nginx 反向代理配置
├── docker-compose.yml          # Docker 编排
├── AGENTS.md                   # AI Agent 行为指令
└── CLAUDE.md                   # Claude Code 项目上下文
```

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 管理员 |

## 开发规范

- [API 设计规范](doc/api-convention.md)
- [开发规范](doc/dev-guide.md)
- [部署指南](doc/deploy-guide.md)

## License

MIT
