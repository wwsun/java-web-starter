# frontend/AGENTS.md — 前端开发规范

## 技术栈

- **框架**: React 19 + TypeScript（严格模式）
- **构建**: Vite
- **样式**: TailwindCSS v4 + **shadcn/ui** 组件库
- **设计**: 支持现代化、高美感的 UI 设计（参考 `frontend-design` 规范）
- **路由**: React Router v6
- **HTTP**: Axios（封装在 `src/api/client.ts`）
- **状态管理**: Zustand

## 代码规范

1. **TypeScript 严格模式**：禁止使用 `any`
2. **组件命名**: PascalCase（`MeetingRoomList.tsx`）
3. **API 请求**：统一通过 `src/api/client.ts` 发起
4. **状态管理**：使用 Zustand store，文件放在 `src/stores/`
5. **样式**：使用 TailwindCSS utility classes

## 新增页面模板

在 `src/pages/` 下创建页面，并在 `src/router/index.tsx` 中注册路由。

## 关键文件

- `vite.config.ts` — Vite 配置（API 代理在此）
- `src/api/client.ts` — Axios 实例（Token 注入、401 自动换牌、X-Request-Id 链路追踪注入）
- `src/router/index.tsx` — 路由配置（**Hash Router 模式**，URL 形如 `/#/login`）
- `src/mocks/handlers.ts` — MSW Mock 处理器，用于前端无后端独立开发

## 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器（带热更新）
npm run dev

# 启动并启用 Mock 数据（无需后端）
VITE_ENABLE_MOCK=true npm run dev

# 单文件类型检查（推荐，比全量快）
npx tsc --noEmit src/path/to/file.tsx

# 全量类型检查
npx tsc --noEmit

# Lint（单文件）
npx eslint src/path/to/file.tsx

# Lint（全量）
npm run lint

# 运行单个测试文件
npx vitest run src/path/to/file.test.tsx

# 构建
npm run build
```

## 安全和权限

无需提示可直接执行：

- 读取和搜索代码文件
- 单文件类型检查 / lint / 测试
- 启动开发服务器

先询问确认：

- `npm install` 安装或删除依赖包
- 全量构建 `npm run build`
- `git push` 或创建 PR

## PR 检查清单

- 提交信息格式：`type(scope): 描述`（Conventional Commits）
- TypeScript 类型检查通过（无 `any`）
- `npm run lint` 通过
- 无残留 `console.log`（调试专用除外）
- 无硬编码后端 URL（统一通过 `src/api/client.ts`）
