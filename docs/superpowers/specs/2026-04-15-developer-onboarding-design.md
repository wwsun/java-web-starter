# 开发者快速上手体验改进设计

**日期**：2026-04-15  
**范围**：面向国内开发者，覆盖 clone → 首次启动 → 本地开发完整链路  
**不含**：数据库迁移版本管理（E2，延期）

---

## 背景与目标

当前脚手架存在多处"克隆即失败"或"误导性文档"问题，导致陌生开发者无法顺利跑起来。本设计修复全部阻塞级和高优先级问题，目标是：**任意国内开发者 clone 后，按 README 操作，10 分钟内能访问到运行中的应用**。

---

## 一、网络加速（A1、A2、A3）

### A1 — 前端 Dockerfile 镜像修复

**问题**：`frontend/Dockerfile` 第二阶段直连 Docker Hub，国内大概率超时。

**修复**：`frontend/Dockerfile` 第二阶段 FROM 改为：

```dockerfile
FROM docker.m.daocloud.io/library/nginx:alpine
```

### A2 — Maven 国内镜像

**问题**：`pom.xml` 无镜像配置，首次构建默认走 `repo1.maven.org`，国内极慢。

**修复**：项目内新增 `.mvn/settings.xml`，内容如下：

```xml
<settings>
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

所有 Maven 命令统一改为 `mvn ... -s .mvn/settings.xml`，开发者无需手动配置 `~/.m2/settings.xml`。

### A3 — npm 国内镜像

**问题**：无 `.npmrc`，`npm install` 默认走 `registry.npmjs.org`，国内不稳定。

**修复**：新增 `frontend/.npmrc`：

```
registry=https://registry.npmmirror.com
```

---

## 二、Redis 可选化（B1）

**问题**：README 说"Redis 可选"，但 `application-dev.yml` 硬编码 `cache.type: redis`，不启动 Redis 则后端报 `ConnectionRefusedException`。

**修复**：

1. `application-dev.yml` 缓存类型改为内存模式：
   ```yaml
   spring:
     cache:
       type: simple
   ```
   删除原有 `spring.cache.redis.*` 配置块。

2. `application-prod.yml`（已有）保持 `cache.type: redis` 不变，生产 Docker 部署行为不受影响。

3. `docker-compose.yml` 中 `backend` 的 `depends_on` 保留对 `redis` 的依赖（`docker compose up` 走 prod profile，Redis 仍为必要组件）。需要修正的仅是文档措辞。

4. `README` 更新措辞：  
   - 原：`若需启用 Redis 缓存支持：docker compose up -d redis`  
   - 改：`本地开发无需 Redis（dev profile 使用内存缓存）；生产部署（docker compose up）自动启用 Redis`

---

## 三、工具链统一（B2、B3）

**问题**：README 前置条件写"JDK 21"，未提 sdkman；无 mvnw，开发者需自行安装 Maven；版本管理分散。

**修复**：

1. `backend/.sdkmanrc` 已有（`java=21.0.10-tem`、`maven=3.9.9`），保持不动。

2. `README` 本地开发前置条件改为：
   ```
   前置条件：sdkman、Docker 24+、Node.js 20+
   ```
   删除原"JDK 21"单独说明。

3. `README` 在"Step 2：启动后端"之前新增 sdkman 引导：
   ```bash
   # 安装 sdkman（已安装跳过）
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"

   # 在 backend/ 目录自动安装并切换指定 Java + Maven 版本
   cd backend && sdk env install
   ```

4. 后端所有 Maven 命令加上 `-s .mvn/settings.xml`（与 A2 联动）：
   ```bash
   mvn spring-boot:run -s .mvn/settings.xml
   mvn test -s .mvn/settings.xml
   ```

---

## 四、配置引导（C1、C2）

### C1 — `.env.example` 补充

在现有三个变量后追加：

```bash
# CORS 允许来源（本地开发默认 localhost:5173，部署时改为实际域名）
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### C2 — 前端新增 `.env.example`

新建 `frontend/.env.example`：

```bash
# 启用 MSW Mock 数据（无需后端时设为 true）
VITE_ENABLE_MOCK=false
```

---

## 五、文档修正（D1、D2）

### D1 — 首次启动时间预期

`README` 修改"约 2 分钟"的表述：

> 首次构建需下载依赖，耗时约 **10~20 分钟**；后续重启约 1 分钟。  
> 可用 `docker compose logs -f backend` 实时观察启动进度，看到 `Started Application` 即表示就绪。

### D2 — fork 后包名替换警告

在 `README` 技术栈表格上方新增提示块：

```markdown
> **使用前必读**：本仓库为脚手架模板，后端包名默认为 `com.music163.starter`。
> fork 新项目后，请先按 [TODO.md](TODO.md) 第 0 步完成包名替换，再开始开发。
```

---

## 六、统一启动入口（E1）

**问题**：本地开发需开三个终端，步骤有顺序依赖，无统一入口。

**修复**：在项目根目录新增 `Makefile`：

```makefile
.PHONY: dev-db backend frontend up down test logs help

MVN_OPTS = -s .mvn/settings.xml

help:          ## 显示帮助
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

dev-db:        ## 启动本地开发数据库（MySQL）
	docker compose up -d mysql

backend:       ## 启动后端（需先运行 dev-db）
	cd backend && sdk env && mvn spring-boot:run $(MVN_OPTS)

frontend:      ## 启动前端开发服务器
	cd frontend && npm install && npm run dev

up:            ## 全栈 Docker 启动（生产模式）
	docker compose up -d --build

down:          ## 停止所有 Docker 服务
	docker compose down

test:          ## 运行前后端测试
	cd backend && mvn test $(MVN_OPTS)
	cd frontend && npm run test:run

logs:          ## 查看后端日志
	docker compose logs -f backend
```

`README` 本地开发步骤更新为：

```bash
make dev-db       # Step 1：启动数据库
make backend      # Step 2：启动后端（新终端）
make frontend     # Step 3：启动前端（新终端）
```

---

## 变更文件清单

| 文件 | 操作 | 对应问题 |
|---|---|---|
| `frontend/Dockerfile` | 修改第二阶段 FROM | A1 |
| `.mvn/settings.xml` | 新增 | A2 |
| `frontend/.npmrc` | 新增 | A3 |
| `backend/src/main/resources/application-dev.yml` | 修改 cache.type | B1 |
| `README.md` | 多处修改 | B1、B2、D1、D2、E1 |
| `.env.example` | 追加变量 | C1 |
| `frontend/.env.example` | 新增 | C2 |
| `Makefile` | 新增 | E1 |

---

## 不在本次范围内

- **E2 数据库迁移版本管理**（引入 Flyway）：延期，需单独立项
- `nginx/default.conf`：无需修改
- `docker-compose.yml`：仅 B1 涉及文档侧修正，compose 文件本身不改
