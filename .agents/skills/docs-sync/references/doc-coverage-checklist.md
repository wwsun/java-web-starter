# 文档覆盖检查清单

在扫描选定范围（main 分支 = 全量，或当前分支 diff）并验证文档覆盖率时使用此清单。

## 后端扫描目标

- **新业务模块**：`backend/src/main/java/.../module/` 下的新目录
- **新 REST 接口**：带有 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@PatchMapping`、`@DeleteMapping` 的方法
- **请求/响应类型**：`dto/` 下的 `*Request.java` 和 `vo/` 下的 `*VO.java`
- **配置项**：`application*.yml` 中新增的 key 或修改的默认值
- **Maven 依赖**：`pom.xml` 中新增或版本变更的依赖
- **安全规则**：`SecurityConfig` 中新增的权限或路径白名单
- **公共工具**：`common/` 下新增的工具类、异常类型、全局配置

## 前端扫描目标

- **新页面**：`frontend/src/pages/` 下的新文件
- **新路由**：`frontend/src/router/index.tsx` 中的新条目
- **新 API 服务**：`frontend/src/api/` 下的新文件
- **新 Store**：`frontend/src/stores/` 下的新 Zustand store
- **新 Hook**：`frontend/src/hooks/` 下的新自定义 Hook
- **环境变量**：`vite.config.ts` 或 `.env*` 中新增的 `VITE_*` 变量

## 基础设施扫描目标

- `docker-compose.yml` 中新增或修改的服务
- 端口映射或环境变量的变更
- `nginx/` 下配置文件的变更
- 新增的部署脚本或 CI 配置

## AGENTS.md / CLAUDE.md 审查目标

- 技术栈版本号（Java、Spring Boot、React、Vite、Node.js 等）
- 构建和运行命令的准确性
- 文件路径和目录结构的描述
- 禁止事项（是否仍然有效）
- 基础设施命令（如 docker compose 命令）
- 包名和模块名称

## 逐页文档检查

| 文档 | 检查重点 |
|------|---------|
| `doc/architecture.md` | 技术栈版本、模块边界、系统图 |
| `doc/dev-guide.md` | Git 工作流、提交规范、代码审查清单、构建命令 |
| `doc/api-convention.md` | 响应格式、认证流程、分页参数、状态码 |
| `doc/how-to-add-module.md` | 模块目录结构模板、每步操作步骤、测试命令 |
| `doc/idea-setup.md` | JDK 版本、插件版本、配置路径 |
| `doc/deploy-guide.md` | 部署步骤是否匹配当前 `docker-compose.yml` |
| `doc/sql/` | SQL 文件是否与现有 Entity 表定义一致 |
| `backend/AGENTS.md` | 技术栈版本、包结构、代码规范、测试命令 |
| `frontend/AGENTS.md` | 技术栈版本、目录约定、开发命令、环境变量 |

## 代码优先映射规则

- 优先更新现有文档页面，而非新建文件。
- 新模块出现 → 检查 `how-to-add-module.md` 模板是否仍然适用。
- `pom.xml` 版本变化 → 更新 `backend/AGENTS.md` 中的技术栈版本。
- 新的前端依赖 → 检查 `frontend/AGENTS.md` 是否需要补充说明。
- `docker-compose.yml` 变化 → 检查 `doc/deploy-guide.md` 和根 `AGENTS.md`。

## 文档过时的警示信号

- 文档中引用的 Java 类或包路径在代码库中不存在
- 文档中的命令执行后报错
- 文档中的 API 路径与 Controller 中的 `@RequestMapping` 不一致
- 文档描述的 docker-compose 服务已被移除
- AGENTS.md 中的版本号与 `pom.xml` 或 `package.json` 不符
- 文档中提到的 SQL 表或字段与 Entity 类不匹配

## Diff 模式指引（feature 分支）

- 使用 `git diff main...HEAD` 限定分析范围。
- 新增文件是文档覆盖缺失的强信号。
- 修改了配置文件（`pom.xml`、`application.yml`、`vite.config.ts`）往往意味着需要更新文档。
- 删除的文件或类意味着文档中可能存在过时引用需要清除。

## 修改原则

- 保持与现有文档相同的语气、格式和缩进风格。
- 更新交叉引用（如某章节路径变更）。
- AGENTS.md 中的修改要保持现有的结构层次。
- 不引入未经验证的新约定或规则。
