# Vitest 升级方案 (v3.x 升级至 v4.x)

## 背景与动机
前端工程 (`frontend/`) 当前使用的 Vitest 版本为 `^3.2.4`。为了利用 Vitest 4.x 版本带来的性能提升、更好的 TypeScript 类型推断支持（通过新的 `test.extend` 模式）以及优化的模块解析器，我们需要将 Vitest 及其配套工具升级到最新的稳定版（当前为 `v4.1.4`）。

## 影响范围与上下文
- **目标工程**: `frontend/`
- **受影响依赖**: `vitest`, `@vitest/ui`
- **现有配置**: `frontend/vite.config.ts` 中的 `test` 配置项较简单，仅使用了 `environment: 'jsdom'`, `globals: true`, `setupFiles`, 和 `exclude`，这些在 4.x 版本中完全兼容，无废弃 API 被命中。
- **环境依赖**: Node.js 版本目前为 `v24.12.0`，完全满足 Vitest 4.x 要求的 Node.js `>= 20.0.0`。Vite 版本为 `^8.0.4`，满足 Vite `>= 6.0.0` 的要求。

## 实施步骤

1. **更新依赖版本**
   修改 `frontend/package.json` 中的 `devDependencies`，将 Vitest 及其 UI 插件更新到 `^4.1.4`：
   ```json
   "devDependencies": {
     // ...
     "@vitest/ui": "^4.1.4",
     // ...
     "vitest": "^4.1.4"
   }
   ```

2. **重新安装依赖**
   在 `frontend/` 目录下执行依赖更新：
   ```bash
   npm install
   ```

3. **执行测试验证**
   执行以下命令确保所有现有测试用例（如组件、Hooks、Stores 等）能正常运行并通过：
   ```bash
   npm run test:run
   ```
   检查并确保现有的 Mock 逻辑（如针对 `react-router-dom` 的 Mocking 和 MSW API Mocking）在新版本下正常工作。

## 验证指标
- 依赖安装无冲突或错误。
- 所有的单元测试（含 UI 渲染和自定义 Hooks 测试）需 100% 成功通过（Exit Code 为 0）。
- 不需要在现阶段修改现有的测试用例代码。

## 回滚策略
若升级后出现无法简单修复的重大兼容性问题，则通过 git 将 `frontend/package.json` 及 `frontend/package-lock.json` 恢复至升级前状态。