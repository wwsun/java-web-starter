# Java Web Starter

> 本项目是一套可复用的前后端分离 Web 工程脚手架，面向企业内部系统开发（如会议室预订），用于新项目通过 fork / clone 快速启动。

## 技术栈

| 层级         | 技术                                                                              |
| ------------ | --------------------------------------------------------------------------------- |
| **后端**     | Spring Boot 3.2 · JDK 21 · MyBatis-Plus · Spring Security + JWT · Redis · Knife4j |
| **前端**     | Vite · React 19 · TypeScript · TailwindCSS v4 · React Router v6 · Axios · Zustand |
| **基础设施** | Docker Compose · MySQL 8.4 · Redis 7 · Nginx                                      |

## 快速启动

### 方式一：快速体验（单服务启动）

前端构建产物内嵌到 Spring Boot 中，只需启动一个后端服务即可体验完整功能。

```bash
# 1. 启动数据库和缓存
docker compose up -d mysql redis

# 2. 构建前端并拷贝到后端静态资源目录
cd frontend && npm install && npm run build
cp -r dist/* ../backend/src/main/resources/static/

# 3. 启动后端
cd ../backend && mvn spring-boot:run

# 4. 访问 http://localhost:8080/api
#    所有页面和 API 由同一个服务提供
```

### 方式二：本地开发（前后端分离）

日常开发推荐前后端分别启动，支持热更新。

#### 前置条件

- JDK 21（推荐使用 [SDKMAN](https://sdkman.io/) 管理）
- Node.js 20+
- Docker（用于 MySQL 和 Redis）
- **IntelliJ IDEA**（推荐 Ultimate 版，内置前端开发支持）

#### 启动步骤

```bash
# 1. 启动数据库和缓存
docker compose up -d mysql redis
```

**后端**：

1. 使用 IDEA 打开项目根目录 `java-web-starter/`
2. 将 `backend/` 标记为 Maven 模块，等待依赖下载完成
3. 运行 `StarterApplication.main()` 启动应用
4. 访问 API 文档：http://localhost:8080/api/doc.html

**前端**：

1. 在 IDEA 内置终端中执行：

```bash
cd frontend
npm install
npm run dev
```

2. 访问前端：http://localhost:5173（API 请求自动代理到后端）

#### 启用 Mock 数据（可选）

前端支持通过 MSW 进行独立开发，无需启动后端：

```bash
cd frontend
VITE_ENABLE_MOCK=true npm run dev
```

### 方式三：Docker Compose（部署）

```bash
# 克隆项目
git clone <repo-url>
cd java-web-starter

# 一键启动所有服务
docker compose up -d

# 访问
# 前端: http://localhost
# 后端 API: http://localhost/api
# API 文档: http://localhost:8080/api/doc.html
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

| 用户名 | 密码     | 角色   |
| ------ | -------- | ------ |
| admin  | admin123 | 管理员 |

## 开发规范

- [API 设计规范](doc/api-convention.md)
- [开发规范](doc/dev-guide.md)
- [部署指南](doc/deploy-guide.md)

## License

MIT
