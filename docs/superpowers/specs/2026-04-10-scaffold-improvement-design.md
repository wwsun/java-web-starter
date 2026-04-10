# java-web-starter 脚手架改进设计文档

**日期**：2026-04-10  
**状态**：已批准  
**背景**：脚手架用于内部项目快速初始化，同时作为全栈技能考核基线。考核维度包括功能完整性、技术实现质量、代码可维护性、可测试性、Spec Driven、开发指南、AI 工具链使用。

---

## 改进范围

| 优先级 | 改进项 | 说明 |
|--------|--------|------|
| P0 | RBAC 权限体系 | 补全角色/权限模型，提供候选人可参考的授权范本 |
| P0 | 前端 Vitest 单元测试 | 引入测试框架并提供示范测试文件 |
| P0 | Playwright E2E 测试 | 提供完整登录流程的端到端测试示范 |
| P1 | 安全问题修复 | 修复 CORS 过宽、Token 未自动刷新、Dockerfile JRE 版本错误 |
| P1 | Spec 模板 + Walkthrough 文档 | 提供模块规格模板和新模块开发完整指引 |

不在范围内：Flyway 数据库迁移、AGENTS.md 增强、GitHub Actions CI。

---

## 第一节：RBAC 权限体系

### 数据库模型

新增 2 张表，与现有 `users` 表组成简洁 RBAC：

```sql
-- 角色表
CREATE TABLE roles (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码，如 ADMIN / USER'
);

-- 用户-角色关联表
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
```

**初始化数据**：
- 角色：`ADMIN`（管理员）、`USER`（普通用户）
- 默认 `admin` 账号分配 `ADMIN` 角色
- 新注册用户默认分配 `USER` 角色

### 后端实现

**新增模块** `module/role/`：

```
module/role/
├── controller/RoleController.java   # GET /roles, POST /users/{id}/roles
├── service/RoleService.java
├── service/impl/RoleServiceImpl.java
├── mapper/RoleMapper.java
├── entity/Role.java
├── dto/AssignRoleRequest.java
└── vo/RoleVO.java
```

**安全层变更**：
- `CustomUserDetailsService`：加载用户时联表查询角色，填入 `SimpleGrantedAuthority("ROLE_" + code)`
- `SecurityConfig`：添加 `@EnableMethodSecurity` 注解
- `UserController.deleteUser`：添加 `@PreAuthorize("hasRole('ADMIN')")` 作为示范
- `UserVO`：新增 `roles: List<String>` 字段（角色 code 列表）

**不引入**：自定义注解，直接使用 Spring Security 原生 `@PreAuthorize`，更标准。

### 前端实现

- `useAuthStore`：登录后调用 `/api/users/me`，将 `roles` 字段存入 store
- 新增 `hooks/usePermission.ts`：`usePermission(role: string): boolean`
- `RequireAuth` 组件：增加可选 `requiredRole?: string` 属性，无权限时重定向 403 页面
- `DashboardPage`：增加"用户管理"入口，使用 `usePermission('ADMIN')` 控制显隐，作为示范

---

## 第二节：前端测试体系

### Vitest 单元测试

**新增依赖**（devDependencies）：
- `vitest`
- `@testing-library/react`
- `@testing-library/user-event`
- `jsdom`

**配置变更**：
- `vite.config.ts`：增加 `test: { environment: 'jsdom', globals: true }` 配置块
- `package.json`：增加 `"test": "vitest"` 和 `"test:ui": "vitest --ui"` 脚本

**新增示范测试文件**：

```
frontend/src/
├── stores/useAuthStore.test.ts
│     # 覆盖：setTokens 存储到 localStorage、logout 清除状态、
│     #       页面刷新后从 localStorage 恢复 token
├── hooks/usePermission.test.ts
│     # 覆盖：有对应角色返回 true、无角色返回 false、未登录返回 false
└── pages/LoginPage.test.tsx
      # 覆盖：表单渲染、用户名/密码输入、提交调用 API、
      #       登录失败显示错误信息、登录成功跳转路由
      # Mock：使用已有 MSW handlers.ts
```

### Playwright E2E 测试

**新增依赖**（devDependency）：`@playwright/test`

**新增配置文件** `frontend/playwright.config.ts`：
- `baseURL: 'http://localhost:5173'`
- 单浏览器（Chromium），保持示范简洁

**新增测试文件** `frontend/e2e/auth.spec.ts`：

```
覆盖流程：
1. 访问首页 → 重定向到 /login
2. 输入 admin / admin123
3. 点击登录 → 跳转到 /（Dashboard）
4. 页面出现用户名
5. 点击登出 → 跳转回 /login
```

**新增脚本**（`package.json`）：
- `"test:e2e": "playwright test"`
- `"test:e2e:ui": "playwright test --ui"`

---

## 第三节：安全问题修复

### 3.1 CORS 配置收紧

**问题**：`setAllowedOriginPatterns(List.of("*"))` 允许任意来源。

**修复**：
1. `application-dev.yml` 新增：
   ```yaml
   app:
     cors:
       allowed-origins: "http://localhost:5173"
   ```
2. `application-prod.yml` 新增：
   ```yaml
   app:
     cors:
       allowed-origins: "${APP_CORS_ALLOWED_ORIGINS}"
   ```
3. `SecurityConfig`：通过 `@Value("${app.cors.allowed-origins}")` 注入，替换通配符

### 3.2 Refresh Token 自动刷新

**问题**：`client.ts` 响应拦截器在 401 时直接跳转登录，未尝试 Token 续期。

**修复**：在 `client.ts` 的 401 处理逻辑中增加刷新重试：

```
收到 401 响应
  └─ isRefreshing === false？
       ├─ 是：设 isRefreshing=true，调用 refreshToken()
       │      ├─ 成功：更新 store token，重试原请求，isRefreshing=false
       │      └─ 失败：logout() + 跳转 /login，isRefreshing=false
       └─ 否：将原请求加入等待队列，刷新完成后统一重试
```

用 `isRefreshing` 标志 + 请求等待队列防止并发多次刷新。

### 3.3 Dockerfile JRE 版本修复

**问题**：`backend/Dockerfile` 运行阶段使用 `eclipse-temurin:11-jre-alpine`，与项目 JDK 21 要求不符。

**修复**：将运行阶段基础镜像改为 `eclipse-temurin:21-jre-alpine`。

---

## 第四节：Spec 模板 + 新模块开发 Walkthrough

### 新增目录结构

```
doc/
├── templates/
│   ├── module-spec-template.md     # 业务模块规格模板
│   └── api-endpoint-template.md   # 单接口设计模板
└── how-to-add-module.md           # 新模块开发完整指引
```

### module-spec-template.md 结构

```markdown
# [模块名] 功能规格

## 需求描述
简要描述模块要解决的问题。

## 数据模型
ER 图或字段表，说明表名、字段、约束、索引。

## API 设计
每个端点包含：URL、Method、请求参数、成功响应、错误码。

## 前端页面
列出页面/组件，描述核心交互。

## 测试用例
| 场景 | 前置条件 | 操作 | 期望结果 |
正常流程 / 边界条件 / 异常情况各至少一条。

## 待确认问题
开发前需要澄清的问题列表。
```

### api-endpoint-template.md 结构

```markdown
## POST /api/xxx

**描述**：
**权限**：需要 `xxx:write` 权限

**请求体**：
**成功响应** 200：
**错误响应**：
- 400：参数校验失败
- 401：未登录
- 403：无权限
- 404：资源不存在
```

### how-to-add-module.md 内容结构

以**会议室预订**为虚构示例，覆盖完整 7 步流程：

1. **写 Spec**：使用 `module-spec-template.md`，确认数据模型和 API 边界
2. **写 SQL**：在 `doc/sql/` 新增建表语句，本地手动执行
3. **后端**：按 `module/user/` 结构创建 entity → mapper → service → controller
4. **加权限**：在 `doc/sql/` 补充 permission 初始化数据，`@PreAuthorize` 注册新权限
5. **前端**：新增 API 函数 → 页面组件 → 注册路由，可选新增 Zustand store
6. **写测试**：Service 单元测试 → Controller 切片测试 → Vitest 组件测试
7. **提交前自测 Checklist**：
   - [ ] `mvn test` 全绿
   - [ ] `npm test` 全绿
   - [ ] Knife4j 文档可访问且接口描述完整
   - [ ] 无硬编码 URL 或密钥
   - [ ] VO 不含 `password`、`deleted` 等敏感字段
   - [ ] 新 SQL 已添加到 `doc/sql/`

---

## 文件变更汇总

### 后端新增/修改

| 文件 | 变更类型 |
|------|----------|
| `doc/sql/init.sql` | 修改：追加 RBAC 建表语句和初始化数据 |
| `module/role/**` | 新增：角色模块（7 个文件） |
| `module/user/vo/UserVO.java` | 修改：新增 `roles` 字段 |
| `security/CustomUserDetailsService.java` | 修改：加载角色和权限 |
| `security/SecurityConfig.java` | 修改：启用 `@EnableMethodSecurity`，收紧 CORS |
| `module/user/controller/UserController.java` | 修改：`deleteUser` 加 `@PreAuthorize` |
| `resources/application-dev.yml` | 修改：新增 `app.cors.allowed-origins` |
| `resources/application-prod.yml` | 修改：新增 `app.cors.allowed-origins` |
| `Dockerfile` | 修改：JRE 11 → JRE 21 |

### 前端新增/修改

| 文件 | 变更类型 |
|------|----------|
| `stores/useAuthStore.ts` | 修改：新增 `roles` 字段 |
| `hooks/usePermission.ts` | 新增 |
| `router/index.tsx` | 修改：`RequireAuth` 支持 `requiredRole` |
| `pages/DashboardPage.tsx` | 修改：新增管理员入口示范 |
| `api/client.ts` | 修改：401 自动刷新逻辑 |
| `vite.config.ts` | 修改：新增 vitest 配置 |
| `playwright.config.ts` | 新增 |
| `stores/useAuthStore.test.ts` | 新增 |
| `hooks/usePermission.test.ts` | 新增 |
| `pages/LoginPage.test.tsx` | 新增 |
| `e2e/auth.spec.ts` | 新增 |
| `package.json` | 修改：新增测试脚本和依赖 |

### 文档新增

| 文件 | 变更类型 |
|------|----------|
| `doc/templates/module-spec-template.md` | 新增 |
| `doc/templates/api-endpoint-template.md` | 新增 |
| `doc/how-to-add-module.md` | 新增 |
