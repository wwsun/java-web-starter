---
name: docs-sync
description: 分析代码变更，找出 doc/ 目录和 AGENTS.md/CLAUDE.md 中缺失、过时或不准确的文档，并提出修复建议。当被要求检查文档覆盖率、同步文档与代码、更新项目文档，或者你发现代码变更可能导致文档失效时，都应主动使用此 skill。在确认前不修改任何文件，先给出报告。
---

# Docs Sync

## 概述

对比代码库当前状态与 `doc/`、`AGENTS.md`、`CLAUDE.md` 等文档，识别文档覆盖空白和内容不准确之处，然后提出针对性改进方案。

## 文档范围

以下文件在本次审查和更新范围内：

**项目文档**
- `doc/architecture.md` — 架构总览
- `doc/dev-guide.md` — 开发规范（Git 分支、提交格式、审查清单）
- `doc/api-convention.md` — API 设计规范
- `doc/how-to-add-module.md` — 新增业务模块流程
- `doc/idea-setup.md` — IDE 环境配置
- `doc/deploy-guide.md` — 部署指南
- `doc/java-coding-guidelines.md` — Java 编码规范
- `doc/sql/` — 数据库初始化 SQL

**Agent 配置文件**
- `AGENTS.md` / `CLAUDE.md` — 根目录 AI Agent 行为指令
- `backend/AGENTS.md` / `backend/CLAUDE.md` — 后端开发规范
- `frontend/AGENTS.md` / `frontend/CLAUDE.md` — 前端开发规范

## 工作流

### 第一步：确认分析范围

- 识别当前分支和默认分支（通常为 `main`）。
- 若在 `main` 分支：全量审查所有文档。
- 若在 feature 分支：用 `git diff main...HEAD` 缩小范围，只分析变更部分。
- 不要切换分支；需要查看主分支文件时使用 `git show main:<path>`。

### 第二步：构建特性清单

**后端扫描目标**（`backend/src/` 下）：
- `module/` 下的新业务模块（entity、mapper、service、controller）
- 新增或修改的 REST 接口（`@GetMapping`、`@PostMapping` 等）
- `application*.yml` 中新增的配置项或修改的默认值
- `pom.xml` 中新增或升级的依赖
- `SecurityConfig` 中安全规则的变更
- `common/` 下新增的公共工具类

**前端扫描目标**（`frontend/src/` 下）：
- `pages/` 下的新页面
- `router/index.tsx` 中的新路由
- `api/` 下的新 API 服务文件
- `stores/` 下的新 Zustand store
- `vite.config.ts` 或 `.env*` 中新增的环境变量

**基础设施扫描目标**：
- `docker-compose.yml` 的变更（新服务、端口变更、新环境变量）
- `nginx/` 配置变更
- 新增的部署相关脚本

### 第三步：文档优先扫描（逐页检查）

逐一检查 `doc/` 下每个文档，寻找与当前代码不符的内容：
- 已失效的文件路径、类名、包名
- 过时的版本号或命令
- 已移除的特性或配置仍在文档中保留
- 新增的代码模式未在对应文档中体现

重点检查 `AGENTS.md` 类文件中的技术栈版本号、构建命令、文件路径是否仍然准确。

### 第四步：代码优先扫描（特性映射）

对清单中的每一项，确定对应文档：
- 新模块 → `doc/how-to-add-module.md` 中的模板是否仍然适用
- 新 API 端点 → `doc/api-convention.md` 中的规范是否覆盖
- 技术栈版本变化 → `backend/AGENTS.md` 或 `frontend/AGENTS.md` 是否需要更新
- 基础设施变更 → `doc/deploy-guide.md` 是否反映了最新状态

### 第五步：识别差距和错误

- **缺失**：代码中存在但文档未提及的功能或配置。
- **过时/不准确**：文档中的名称、路径、默认值与代码不符。
- **AGENTS.md 问题**：AI Agent 指令中技术版本、命令或约定已不准确。

### 第六步：输出报告并请求确认

生成 Docs Sync Report（见下方格式），然后询问用户是否批准执行变更。**不要在获得确认前修改任何文件。**

### 第七步：应用变更（确认后）

- 直接编辑相关文档，保持现有格式和语气风格。
- 除非某个话题明显没有归属，否则优先更新现有文档而非新建文件。
- 更新 AGENTS.md 时，保持与现有内容一致的结构和标题风格。
- 修改后进行简单的一致性检查（如路径引用是否仍然有效）。

## 输出格式

使用以下模板汇报发现：

```
Docs Sync Report

## 文档优先发现
- [文档文件] → [缺失或过时的内容] → 证据（代码路径:行号）+ 建议修改点

## 代码优先差距
- [特性名称] → 证据（文件:行号） → 建议更新的文档文件/章节

## AGENTS.md / CLAUDE.md 问题
- [文件] → [问题描述] → [正确信息] → 证据

## 结构性建议（可选）
- [建议变更] + 原因

## 拟执行的修改
- [文档文件] → [简要变更摘要]

## 问题（如有）
```

## 参考资料

- `references/doc-coverage-checklist.md` — 扫描目标的详细检查清单
