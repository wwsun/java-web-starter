## [METHOD] /api/[path]

**描述**：[接口功能简述]

**权限**：登录用户 / 需要 `ROLE_ADMIN`

---

### 请求

**请求头**：
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**路径参数**（如有）：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 资源 ID |

**请求体**（POST/PUT）：
```json
{
  "field1": "string，必填，最大 100 字符",
  "field2": 0
}
```

---

### 响应

**成功** `200 OK`：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "field1": "value"
  }
}
```

**错误**：

| code | 说明 |
|------|------|
| 400 | 参数校验失败 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
