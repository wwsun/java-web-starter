# [模块名] 功能规格

> **使用方式**：复制此文件到 `doc/specs/` 中，按实际需求填写，提交前删除所有 `> 提示` 注释。

## 1. 需求描述

> 用 2-3 句话描述该模块解决的问题和业务价值。

## 2. 数据模型

> 描述涉及的数据库表。遵循项目约定：表名 snake_case，含 `created_at`/`updated_at`/`deleted` 字段。

### 表名：`xxx_records`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 名称 |
| status | TINYINT | DEFAULT 1 | 状态：0-禁用 1-正常 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

## 3. API 设计

### 3.1 端点列表

| Method | URL | 说明 | 权限 |
|--------|-----|------|------|
| GET | /api/xxx | 分页查询 | 登录用户 |
| POST | /api/xxx | 创建 | 登录用户 |
| GET | /api/xxx/{id} | 查询详情 | 登录用户 |
| PUT | /api/xxx/{id} | 更新 | ROLE_ADMIN |
| DELETE | /api/xxx/{id} | 软删除 | ROLE_ADMIN |

### 3.2 端点详情

> 每个端点复制 `api-endpoint-template.md` 并填写。

## 4. 前端页面

| 路径 | 组件 | 功能 | 权限 |
|------|------|------|------|
| /xxx | XxxListPage | 列表 + 搜索 + 新增/编辑/删除 | 登录用户 |

## 5. 测试用例

| 场景 | 前置条件 | 操作 | 期望结果 |
|------|----------|------|---------|
| 正常创建 | 已登录，body 合法 | POST /api/xxx | 200，返回新建记录 |
| 参数校验失败 | 已登录 | POST /api/xxx（缺少必填字段） | 400 |
| 未登录访问 | 未登录 | GET /api/xxx | 401 |
| 无权限操作 | 已登录 USER 角色 | DELETE /api/xxx/1 | 403 |

## 6. 待确认问题

- [ ] 问题一
- [ ] 问题二
