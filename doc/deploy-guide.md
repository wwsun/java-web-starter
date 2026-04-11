# 部署指南

## Docker Compose 部署

### 环境要求

- Docker 24+
- Docker Compose v2

### 配置环境变量

创建 `.env` 文件：

```bash
# MySQL
MYSQL_ROOT_PASSWORD=your-strong-password

# JWT
JWT_SECRET=your-256-bit-secret-key-change-in-production
```

### 部署步骤

```bash
# 1. 构建并启动默认服务（不含 Redis）
docker compose up -d --build

# 如需启用 Redis 缓存
# SPRING_CACHE_TYPE=redis docker compose --profile cache up -d --build

# 2. 查看服务状态
docker compose ps

# 3. 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 4. 停止服务
docker compose down

# 5. 停止并清除数据
docker compose down -v
```

### 服务端口

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| Nginx | 80 | 反向代理入口 |
| Backend | 8080 | Spring Boot API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 可选缓存服务 |

## 环境变量说明

### 后端环境变量

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `dev` |
| `SPRING_DATASOURCE_URL` | 数据库 JDBC URL | - |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | - |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | - |
| `SPRING_CACHE_TYPE` | 缓存实现类型 | `simple` |
| `SPRING_DATA_REDIS_HOST` | Redis 主机 | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 端口 | `6379` |
| `JWT_SECRET` | JWT 签名密钥（≥256位） | - |

### 前端环境变量

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `VITE_ENABLE_MOCK` | 启用 MSW Mock | `false` |

## CI/CD 接入指引

### GitHub Actions 模板

```yaml
name: CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Build & Test
        working-directory: backend
        run: mvn clean verify -B

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install & Build
        working-directory: frontend
        run: |
          npm ci
          npm run build

  deploy:
    needs: [backend, frontend]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Deploy
        run: |
          # 在此添加部署脚本
          echo "Deploying to production..."
```

## 数据库管理

### 备份

```bash
# 导出数据库
docker compose exec mysql mysqldump -u root -p starter_db > backup.sql

# 导入数据库
docker compose exec -T mysql mysql -u root -p starter_db < backup.sql
```

### 数据库迁移

目前使用手动 SQL 脚本管理，建议后续引入 Flyway 做版本化管理：

```
doc/sql/
├── init.sql          # 初始化脚本
├── V2__add_roles.sql # 迭代脚本
└── V3__add_xxx.sql
```
