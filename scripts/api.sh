#!/usr/bin/env bash
# =============================================================================
# scripts/api.sh — 开发环境 API 测试工具
#
# 用途：为 Claude Code 和本地开发提供 token 自动缓存的 API 请求封装，
#        避免每次验证都需要手动登录获取 token。
#
# 使用方式（在项目根目录执行）：
#   source scripts/api.sh                     # 加载工具函数
#   api_get /api/users/me                     # 带 token 的 GET 请求
#   api_post /api/users -d '{"username":"x"}' # 带 token 的 POST 请求
#   api_login                                 # 强制重新登录
#   api_token                                 # 打印当前 token
#
# 环境变量（可覆盖默认值）：
#   API_BASE_URL=http://localhost:8080        # 服务地址
#   API_USERNAME=admin                        # 登录用户名
#   API_PASSWORD=admin123                     # 登录密码
# =============================================================================

# -------------- 配置 --------------
_API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
_API_USERNAME="${API_USERNAME:-admin}"
_API_PASSWORD="${API_PASSWORD:-admin123}"

# token 缓存文件（每个 BASE_URL 独立缓存，避免多环境冲突）
_API_TOKEN_DIR="/tmp/.starter-api"
_API_URL_HASH=$(echo -n "$_API_BASE_URL" | md5 2>/dev/null || echo -n "$_API_BASE_URL" | md5sum | cut -d' ' -f1)
_API_TOKEN_FILE="$_API_TOKEN_DIR/token_${_API_URL_HASH}"
_API_EXPIRY_FILE="$_API_TOKEN_DIR/expiry_${_API_URL_HASH}"

# Access Token 有效期 1h，提前 10min 刷新，缓存有效 50min
_API_TOKEN_TTL=3000

# -------------- 内部函数 --------------

# 检查 jq 是否可用
_api_check_jq() {
  if ! command -v jq &>/dev/null; then
    echo "[api.sh] 提示：安装 jq 可获得格式化输出 (brew install jq)" >&2
    return 1
  fi
  return 0
}

# 格式化输出 JSON（有 jq 用 jq，没有就原样输出）
_api_format() {
  if _api_check_jq 2>/dev/null; then
    jq '.'
  else
    cat
  fi
}

# 执行登录，写入 token 缓存
_api_do_login() {
  mkdir -p "$_API_TOKEN_DIR"
  chmod 700 "$_API_TOKEN_DIR"

  local response
  response=$(curl -s -w "\n__HTTP_STATUS:%{http_code}" \
    -X POST "$_API_BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$_API_USERNAME\",\"password\":\"$_API_PASSWORD\"}" 2>&1)

  local http_status body
  http_status=$(echo "$response" | grep -o '__HTTP_STATUS:[0-9]*' | cut -d: -f2)
  body=$(echo "$response" | sed 's/__HTTP_STATUS:[0-9]*$//')

  if [ "$http_status" != "200" ]; then
    echo "[api.sh] ❌ 登录失败 (HTTP $http_status)：" >&2
    echo "$body" | _api_format >&2
    return 1
  fi

  # 提取 accessToken（兼容有无 jq）
  local token
  if command -v jq &>/dev/null; then
    token=$(echo "$body" | jq -r '.data.accessToken // empty')
  else
    token=$(echo "$body" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  fi

  if [ -z "$token" ]; then
    echo "[api.sh] ❌ 无法从响应中提取 accessToken：" >&2
    echo "$body" >&2
    return 1
  fi

  # 写入缓存
  echo "$token" > "$_API_TOKEN_FILE"
  chmod 600 "$_API_TOKEN_FILE"
  echo $(($(date +%s) + _API_TOKEN_TTL)) > "$_API_EXPIRY_FILE"

  echo "[api.sh] ✅ 登录成功，token 已缓存（有效期约 50 分钟）" >&2
}

# 获取有效的 token（过期或不存在时自动重新登录）
_api_get_token() {
  local now
  now=$(date +%s)

  # 检查缓存是否有效
  if [ -f "$_API_TOKEN_FILE" ] && [ -f "$_API_EXPIRY_FILE" ]; then
    local expiry
    expiry=$(cat "$_API_EXPIRY_FILE" 2>/dev/null)
    if [ -n "$expiry" ] && [ "$now" -lt "$expiry" ]; then
      cat "$_API_TOKEN_FILE"
      return 0
    fi
  fi

  # 缓存不存在或已过期，重新登录
  echo "[api.sh] token 不存在或已过期，正在重新登录..." >&2
  _api_do_login || return 1
  cat "$_API_TOKEN_FILE"
}

# -------------- 公开函数 --------------

# 强制重新登录（刷新 token 缓存）
api_login() {
  rm -f "$_API_TOKEN_FILE" "$_API_EXPIRY_FILE"
  _api_do_login
}

# 打印当前缓存的 token（不触发自动登录）
api_token() {
  if [ -f "$_API_TOKEN_FILE" ]; then
    echo "Bearer $(cat "$_API_TOKEN_FILE")"
  else
    echo "[api.sh] 无缓存 token，请先运行 api_login" >&2
    return 1
  fi
}

# 通用 API 请求（自动注入 Bearer token）
# 用法：api <METHOD> <PATH> [额外的 curl 参数...]
# 示例：api GET /api/users/me
#       api POST /api/users -d '{"username":"x","password":"y","email":"x@y.com"}'
api() {
  local method="${1:?用法: api <METHOD> <PATH> [curl 参数...]}"
  local path="${2:?用法: api <METHOD> <PATH> [curl 参数...]}"
  shift 2

  local token
  token=$(_api_get_token) || return 1

  curl -s -w "\n__HTTP_STATUS:%{http_code}" \
    -X "$method" \
    "${_API_BASE_URL}${path}" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    "$@" | {
      local response
      response=$(cat)
      local http_status body
      http_status=$(echo "$response" | grep -o '__HTTP_STATUS:[0-9]*' | cut -d: -f2)
      body=$(echo "$response" | sed 's/__HTTP_STATUS:[0-9]*$//')

      # 显示状态码（非 2xx 用醒目样式）
      if [ "${http_status:0:1}" = "2" ]; then
        echo "HTTP $http_status" >&2
      else
        echo "⚠️  HTTP $http_status" >&2
      fi

      echo "$body" | _api_format
    }
}

# 快捷函数
api_get()    { api GET    "$@"; }
api_post()   { api POST   "$@"; }
api_put()    { api PUT    "$@"; }
api_patch()  { api PATCH  "$@"; }
api_delete() { api DELETE "$@"; }

# -------------- 健康检查函数 --------------

# 检查服务是否运行
api_health() {
  local url="${_API_BASE_URL}/actuator/health"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [ "$status" = "200" ]; then
    echo "[api.sh] ✅ 服务正常运行：$_API_BASE_URL"
    curl -s "$url" | _api_format
  else
    echo "[api.sh] ❌ 服务不可达（HTTP $status）：$url" >&2
    return 1
  fi
}

# -------------- 常用 API 快捷操作 --------------

# 查询当前登录用户信息
api_me() {
  echo "--- GET /api/users/me ---" >&2
  api_get /api/users/me
}

# 查询用户列表（支持分页参数）
# 用法：api_users [page=1] [size=10]
api_users() {
  local page="${1:-1}"
  local size="${2:-10}"
  echo "--- GET /api/users?page=$page&size=$size ---" >&2
  api_get "/api/users?page=$page&size=$size"
}

# -------------- 初始化提示 --------------
echo "[api.sh] 已加载 API 工具，目标服务：$_API_BASE_URL" >&2
echo "[api.sh] 可用函数：api / api_get / api_post / api_put / api_patch / api_delete" >&2
echo "[api.sh]           api_login / api_token / api_health / api_me / api_users" >&2
