# 开发规范

> [!IMPORTANT]
> **规范优先级**：本项目遵循《Alibaba Java Coding Guidelines》，但当其与本项目特定的 `doc/dev-guide.md` 或 `doc/api-convention.md` 存在差异时，**请务必以本项目文档为准**。例如：
> - 数据库时间字段请使用 `created_at` / `updated_at` (而非 `gmt_create`)
> - 数据库表名请使用复数名词 (如 `users`)
> - 持久层统一使用 `Mapper` 命名 (而非 `DAO`)

## Git 分支策略

采用简化版 GitFlow：

| 分支 | 命名规范 | 说明 |
|---|---|---|
| `main` | - | 生产分支，保持可部署状态 |
| `develop` | - | 开发分支，日常开发基线 |
| `feature/*` | `feature/add-meeting-room` | 功能分支，从 develop 分出 |
| `fix/*` | `fix/login-redirect` | 修复分支 |
| `release/*` | `release/v1.0.0` | 发布分支 |

### 工作流

1. 从 `develop` 创建 `feature/*` 分支
2. 开发完成后提交 Pull Request 到 `develop`
3. Code Review 通过后 Squash Merge
4. 发布时从 `develop` 创建 `release/*` 分支
5. 测试通过后合入 `main` 并打 Tag

## 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <description>

[optional body]
```

### Type

| Type | 说明 |
|---|---|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响逻辑） |
| `refactor` | 代码重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具变更 |

### 示例

```
feat(auth): add JWT refresh token support
fix(user): fix pagination query returning deleted users
docs(readme): update quick start guide
```

## 代码审查 Checklist

### 通用

- [ ] 代码逻辑正确，符合需求描述
- [ ] 无明显的安全漏洞（SQL 注入、XSS）
- [ ] 错误处理完善，不吞异常
- [ ] 日志级别使用正确（DEBUG/INFO/WARN/ERROR）

### 后端

- [ ] 使用统一响应格式 `Result<T>`
- [ ] 参数校验使用 `@Validated` 注解
- [ ] 数据库操作有事务保证
- [ ] 新增接口有 Knife4j 文档注解
- [ ] 敏感信息不硬编码

### 前端

- [ ] TypeScript 类型完整，无 `any`
- [ ] 组件职责单一，可复用
- [ ] 错误边界处理
- [ ] Loading 状态处理
- [ ] 响应式布局适配

## 后端编码规范

### 包命名

```
com.music163.starter
├── common/          # 通用组件
│   ├── result/      # 统一响应
│   ├── exception/   # 异常处理
│   ├── config/      # 配置类
│   ├── filter/      # Servlet 过滤器（RequestIdFilter、RateLimitFilter）
│   └── aspect/      # AOP 切面（RequestLogAspect）
├── security/        # Spring Security 核心配置
├── auth/            # 认证模块
├── user/            # 用户模块（示例）
└── <module>/        # 业务模块直接放根包，内部不再套子包
    ├── <Name>Controller.java
    ├── <Name>Service.java
    ├── <Name>ServiceImpl.java   # 与接口同级，不套 impl/
    ├── <Name>Mapper.java
    ├── <Name>.java              # Entity
    ├── <Name>VO.java
    └── dto/
        └── <Name>Request.java
```

### 命名约定

- 类名：PascalCase（`UserController`）
- 方法名：camelCase（`findByUsername`）
- 常量：UPPER_SNAKE_CASE（`DEFAULT_PAGE_SIZE`）
- 包名：全小写（`com.music163.starter`）
- 数据库表：snake_case（`meeting_rooms`）
- 数据库字段：snake_case（`created_at`）

## 前端编码规范

### 文件命名

- 组件文件：PascalCase（`LoginPage.tsx`）
- 工具文件：camelCase（`useAuthStore.ts`）
- 样式文件：camelCase（`index.css`）

### 目录约定

```
src/
├── api/          # API 请求封装（client.ts、各模块 API 文件）
├── components/   # 通用组件（ui/ 存放 shadcn 组件）
├── constants/    # 常量定义（路由路径等）
├── hooks/        # 自定义 Hooks
├── layouts/      # 布局组件
├── lib/          # 工具函数（shadcn 的 cn 等）
├── mocks/        # MSW Mock 处理器（前端独立开发）
├── pages/        # 页面组件
├── router/       # 路由配置（Hash Router 模式）
├── stores/       # Zustand 状态管理
└── test-utils/   # 测试工具（MSW server、vitest setup）
```
