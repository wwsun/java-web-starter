# API 设计规范

## URL 命名

- 使用 **复数名词**：`/api/users`、`/api/meeting-rooms`
- 使用 **kebab-case**：`/api/meeting-rooms`（非 camelCase）
- 嵌套资源：`/api/users/{userId}/bookings`
- 避免在 URL 中使用动词（动作通过 HTTP Method 语义体现）

## HTTP Method 语义

| Method | 语义 | 示例 | 幂等性 |
|---|---|---|---|
| GET | 查询资源 | `GET /api/users` | ✅ |
| POST | 创建资源 | `POST /api/users` | ❌ |
| PUT | 全量更新 | `PUT /api/users/1` | ✅ |
| PATCH | 部分更新 | `PATCH /api/users/1` | ✅ |
| DELETE | 删除资源 | `DELETE /api/users/1` | ✅ |

## 状态码使用

| 状态码 | 含义 | 使用场景 |
|---|---|---|
| 200 | 成功 | 请求成功（含查询和更新） |
| 201 | 已创建 | POST 创建成功 |
| 204 | 无内容 | DELETE 成功（可选） |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未认证 | 缺少或无效的 Token |
| 403 | 权限不足 | 已认证但无权限 |
| 404 | 不存在 | 资源未找到 |
| 500 | 内部错误 | 服务端异常 |

## 统一响应格式

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

错误响应：

```json
{
  "code": 1003,
  "message": "用户名或密码错误",
  "data": null
}
```

## 分页参数约定

### 请求参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| page | int | 1 | 页码（从 1 开始） |
| size | int | 10 | 每页数量 |

### 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

## 认证规范

- Token 通过 `Authorization` Header 传递：`Authorization: Bearer <token>`
- Access Token 有效期：1 小时
- Refresh Token 有效期：7 天
- Token 过期返回 `401`，客户端自动跳转登录
