# 部署指南

## 环境要求

- Docker 24+
- Docker Compose v2

---

## 快速部署

### Step 1：配置环境变量

```bash
cp .env.example .env
```

打开 `.env`，填入生产值：

| 变量 | 说明 | 生成命令 |
|------|------|---------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | — |
| `JWT_SECRET` | JWT 签名密钥（≥256 位） | `openssl rand -hex 32` |
| `REDIS_PASSWORD` | Redis 密码（留空=无密码） | — |
| `APP_CORS_ALLOWED_ORIGINS` | 前端来源，多个用逗号分隔 | — |

> `⚠️` `.env` 已加入 `.gitignore`，请勿提交到版本库。

### Step 2：构建并启动

```bash
make up
```

等价于 `docker compose up -d --build`。首次构建需下载依赖，约 10～20 分钟。

### Step 3：验证

```bash
docker compose ps
```

所有服务状态应为 `Up`，访问 `http://your-server-ip:8090`。

---

## 服务端口

| 服务 | 宿主机端口 | 容器端口 | 说明 |
|------|----------|---------|------|
| Nginx | 8090 | 80 | 反向代理入口（前端 + API） |
| Backend | 8080 | 8080 | Spring Boot（调试用，生产可关闭） |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |

> 如需改 Nginx 宿主机端口，修改 `docker-compose.yml` 中 `nginx.ports` 和 `.env` 中 `APP_CORS_ALLOWED_ORIGINS` 里的端口号。

---

## 常用维护命令

```bash
make logs                        # 查看后端日志（实时）
docker compose logs -f frontend  # 查看前端日志
docker compose ps                # 查看服务状态

make down                        # 停止所有服务（保留数据）
docker compose down -v           # 停止并清除持久化数据（慎用，数据不可恢复）

docker compose pull && make up   # 拉取新镜像并更新部署
docker compose restart backend   # 仅重启后端（改了环境变量后用）
```

---

## 环境变量完整说明

### 后端（Spring Boot）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | `prod` |
| `SPRING_DATASOURCE_URL` | 数据库 JDBC URL | 容器内 mysql 服务 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码（同 `MYSQL_ROOT_PASSWORD`） | — |
| `SPRING_DATA_REDIS_HOST` | Redis 主机 | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis 端口 | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 密码 | `""` |
| `APP_CORS_ALLOWED_ORIGINS` | 允许的前端来源（逗号分隔） | `http://localhost:5173,http://localhost:8090` |
| `JWT_SECRET` | JWT 签名密钥（≥256 位） | — |

### 前端（构建时注入）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_ENABLE_MOCK` | 启用 MSW Mock（仅本地开发） | `false` |

---

## CI/CD 接入

GitHub Actions 示例（`.github/workflows/ci.yml`）：

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
      - run: cd backend && mvn clean verify -B

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: cd frontend && npm ci && npm run build

  deploy:
    needs: [backend, frontend]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy
        run: |
          # 在此添加部署脚本（SSH、Docker Hub、云厂商 CLI 等）
          echo "Deploying..."
```

---

## 数据库管理

### 备份与恢复

```bash
# 备份
docker compose exec mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" starter_db > backup.sql

# 恢复
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" starter_db < backup.sql
```

### 重置数据库（清空重建）

```bash
docker compose down -v          # 删除数据卷
make up                         # 重新构建并初始化（init.sql 自动执行）
```

### 数据库迁移

目前使用手动 SQL 脚本管理，建议后续引入 Flyway 做版本化：

```
doc/sql/
├── init.sql          # 初始化脚本（容器首次启动自动执行）
├── V2__add_xxx.sql   # 迭代脚本（手动执行）
└── V3__add_yyy.sql
```
