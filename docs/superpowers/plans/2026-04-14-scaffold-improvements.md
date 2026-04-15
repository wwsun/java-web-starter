# Scaffold Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 3 个脚手架高优缺陷：越权漏洞、缺失测试覆盖、缺失根目录 README 和前端 CRUD 示范页。

**Architecture:** Task 1 为纯文档；Task 2 用 TDD 先写失败测试再加鉴权；Task 3 为现有 RoleController 补测试（仿照 UserControllerTest 模式）；Task 4 扩展前端 API + 新增 UserListPage，为脚手架提供完整 CRUD 参考示范。

**Tech Stack:** Java 21 / Spring Boot 3.2 / JUnit5 / MockMvc / Mockito；React 19 / TypeScript / Vitest / MSW / shadcn/ui / TailwindCSS

---

## 涉及文件

| 操作 | 文件 |
|------|------|
| 新建 | `README.md` |
| 修改 | `backend/src/main/java/com/music163/starter/user/UserController.java:48` |
| 修改 | `backend/src/test/java/com/music163/starter/user/UserControllerTest.java` |
| 新建 | `backend/src/test/java/com/music163/starter/role/RoleControllerTest.java` |
| 修改 | `frontend/src/api/user.ts` |
| 新建 | `frontend/src/pages/UserListPage.tsx` |
| 新建 | `frontend/src/pages/UserListPage.test.tsx` |
| 修改 | `frontend/src/router/index.tsx` |
| 修改 | `frontend/src/mocks/handlers.ts` |

---

## Task 1：补根目录 README.md

**Files:**
- Create: `README.md`

- [ ] **Step 1：创建 README.md**

```markdown
# java-web-starter

企业内部管理系统前后端分离脚手架。内置用户管理、角色权限、JWT 双令牌认证，`fork` 后即可快速启动新项目。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.2 · MyBatis-Plus · Spring Security · JJWT |
| 前端 | React 19 · TypeScript · Vite · TailwindCSS v4 · shadcn/ui · Zustand |
| 基础设施 | MySQL 8.4 · Redis 7 · Nginx · Docker Compose |

## 快速启动

**前置条件**：JDK 21、Node.js 20+、Docker

**Step 1：启动数据库**

```bash
docker compose up -d mysql
```

**Step 2：启动后端**

```bash
cd backend
mvn spring-boot:run
# 后端启动后访问 API 文档：http://localhost:8080/doc.html
```

**Step 3：启动前端**

```bash
cd frontend
npm install
npm run dev
# 浏览器访问 http://localhost:5173
# 默认账号：admin / admin123
```

> 前端独立开发（无需后端）：`VITE_ENABLE_MOCK=true npm run dev`

## 项目结构

```
backend/     → Spring Boot 后端（用户/角色模块为示范）
frontend/    → React 前端（Login / Dashboard / 用户列表为示范）
doc/         → 架构设计、API 规范、开发指南
nginx/       → Nginx 反向代理配置
docker-compose.yml
```

## 文档导航

- [架构说明](doc/architecture.md) — 分层约束、双令牌认证机制、可观测性设计
- [API 规范](doc/api-convention.md) — URL 命名、HTTP Method、统一响应格式
- [如何新增模块](doc/how-to-add-module.md) — 7 步从 Spec 到上线的完整流程
- [开发规范](doc/dev-guide.md) — Git 分支策略、提交信息规范、代码审查 Checklist

## 运行测试

```bash
# 后端
cd backend && mvn test

# 前端
cd frontend && npm run test:run
```
```

- [ ] **Step 2：提交**

```bash
git add README.md
git commit -m "docs: add root README with quick start guide"
```

---

## Task 2：修复 GET /api/users/{id} 越权漏洞（TDD）

**Files:**
- Modify: `backend/src/main/java/com/music163/starter/user/UserController.java:48`
- Modify: `backend/src/test/java/com/music163/starter/user/UserControllerTest.java`

**背景：** `GET /api/users/{id}` 当前无鉴权，任意登录用户均可查询任意用户信息。应限制为仅 ADMIN 可调用（用户自己的信息通过 `GET /api/users/me` 获取）。

- [ ] **Step 1：在 UserControllerTest.java 添加失败测试**

在 `UserControllerTest.java` 的 `// ===== DELETE /api/users/{id} =====` 注释之前，新增以下测试方法：

```java
    // ===== GET /api/users/{id} =====

    @Test
    void getUser_whenNonAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME))
                .willReturn(userOnly);

        mockMvc.perform(get("/api/users/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_whenAdmin_shouldReturnUserVO() throws Exception {
        User user = User.builder().id(1L).username("target").nickname("Target").status(1).build();
        given(userService.getById(1L)).willReturn(user);

        mockMvc.perform(get("/api/users/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("target"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
```

- [ ] **Step 2：运行测试，确认 getUser_whenNonAdmin_shouldReturn403 失败**

```bash
cd backend && mvn test -Dtest=UserControllerTest#getUser_whenNonAdmin_shouldReturn403 -q
```

期望输出：`FAIL`（目前无鉴权，非管理员也能拿到 200）

- [ ] **Step 3：在 UserController.java 第 47 行添加 @PreAuthorize**

在 `@GetMapping("/{id}")` 行**上方**插入：

```java
    @Operation(summary = "根据 ID 查询用户（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
```

同时将原有的 `@Operation(summary = "根据 ID 查询用户")` 注解替换掉（合并为上面那行）。

- [ ] **Step 4：运行全部 UserControllerTest，确认全绿**

```bash
cd backend && mvn test -Dtest=UserControllerTest -q
```

期望输出：`BUILD SUCCESS`，所有测试绿色通过。

- [ ] **Step 5：提交**

```bash
git add backend/src/main/java/com/music163/starter/user/UserController.java \
        backend/src/test/java/com/music163/starter/user/UserControllerTest.java
git commit -m "fix(user): restrict GET /users/{id} to ADMIN role to prevent privilege escalation"
```

---

## Task 3：为 RoleController 补测试

**Files:**
- Create: `backend/src/test/java/com/music163/starter/role/RoleControllerTest.java`

**背景：** `RoleController` 有两个端点（`GET /api/roles`、`POST /api/users/{userId}/roles`），完全没有测试覆盖。仿照 `UserControllerTest.java` 的 `@WebMvcTest` 切片测试模式。

- [ ] **Step 1：创建 RoleControllerTest.java**

```java
package com.music163.starter.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.role.dto.AssignRoleRequest;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
import com.music163.starter.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleMapper roleMapper;

    private static final String TOKEN = TestJwtHelper.accessToken(TestJwtHelper.TEST_USERNAME);
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    @BeforeEach
    void setUpAdminMocks() {
        UserDetails admin = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("ADMIN", "USER")
                .build();
        given(jwtTokenProvider.validateAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(TOKEN)).willReturn(TestJwtHelper.TEST_USERNAME);
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(admin);
    }

    // ===== GET /api/roles =====

    @Test
    void listRoles_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRoles_whenNonAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(userOnly);

        mockMvc.perform(get("/api/roles").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRoles_whenAdmin_shouldReturnRoleList() throws Exception {
        RoleVO adminRole = new RoleVO();
        adminRole.setId(1L);
        adminRole.setCode("ADMIN");
        adminRole.setName("管理员");

        given(roleService.listAll()).willReturn(List.of(adminRole));

        mockMvc.perform(get("/api/roles").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].name").value("管理员"));
    }

    // ===== POST /api/users/{userId}/roles =====

    @Test
    void assignRoles_whenNonAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(userOnly);

        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleIds(List.of(1L));

        mockMvc.perform(post("/api/users/2/roles")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignRoles_whenAdmin_shouldReturn200() throws Exception {
        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleIds(List.of(1L, 2L));

        mockMvc.perform(post("/api/users/2/roles")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2：运行 RoleControllerTest，确认全绿**

```bash
cd backend && mvn test -Dtest=RoleControllerTest -q
```

期望输出：`BUILD SUCCESS`，5 个测试全部通过。

> 如果 `RoleVO` 缺少 setter（只有 `@Data` 即有，无需担心）；如果 `AssignRoleRequest.setRoleIds` 不存在，查看 `backend/src/main/java/com/music163/starter/role/dto/AssignRoleRequest.java` 中的实际字段名并修正测试中的 setter 调用。

- [ ] **Step 3：运行全部后端测试，确认不破坏已有测试**

```bash
cd backend && mvn test -q
```

期望输出：`BUILD SUCCESS`

- [ ] **Step 4：提交**

```bash
git add backend/src/test/java/com/music163/starter/role/RoleControllerTest.java
git commit -m "test(role): add WebMvcTest coverage for RoleController (401/403/200 scenarios)"
```

---

## Task 4：前端用户管理 CRUD 示范页

**Files:**
- Modify: `frontend/src/api/user.ts`
- Modify: `frontend/src/mocks/handlers.ts`
- Create: `frontend/src/pages/UserListPage.tsx`
- Create: `frontend/src/pages/UserListPage.test.tsx`
- Modify: `frontend/src/router/index.tsx`

**背景：** 侧边栏已有 "User Management" 导航项（`MainLayout.tsx:11`，指向 `PATHS.USERS = '/users'`），但路由未注册，页面未创建。本任务补全整条链路，作为脚手架的 CRUD 参考示范。

### Step 1：扩展 API 层

- [ ] **Step 1.1：在 `frontend/src/api/user.ts` 添加列表查询和删除函数**

将文件改为：

```typescript
import client from './client';

export interface UserVO {
  id: number;
  username: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  avatar: string | null;
  status: number;
  roles: string[];
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/**
 * 获取当前登录用户信息（含角色）
 */
export function getMe() {
  return client.get<UserVO, UserVO>('/users/me');
}

/**
 * 分页查询用户列表（仅 ADMIN）
 */
export function getUsers(page = 1, size = 10) {
  return client.get<PageResult<UserVO>, PageResult<UserVO>>('/users', {
    params: { page, size },
  });
}

/**
 * 删除用户（仅 ADMIN）
 */
export function deleteUserById(id: number) {
  return client.delete<void, void>(`/users/${id}`);
}
```

### Step 2：补 MSW mock handler（用于 Mock 开发和测试）

- [ ] **Step 2.1：在 `frontend/src/mocks/handlers.ts` 末尾添加 DELETE handler**

在 `handlers` 数组最后一个元素后追加：

```typescript
  // 删除用户（仅示意，Mock 直接返回成功）
  http.delete('/api/users/:id', () => {
    return HttpResponse.json({ code: 200, message: '操作成功', data: null });
  }),
```

### Step 3：写测试（先写，后实现页面）

- [ ] **Step 3.1：创建 `frontend/src/pages/UserListPage.test.tsx`**

```tsx
// frontend/src/pages/UserListPage.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import UserListPage from './UserListPage'
import { useAuthStore } from '@/stores/useAuthStore'
import * as userApi from '@/api/user'
import type { PageResult, UserVO } from '@/api/user'

// Mock API 模块，使测试不依赖网络
vi.mock('@/api/user', async (importOriginal) => {
  const actual = await importOriginal<typeof userApi>()
  return {
    ...actual,
    getUsers: vi.fn(),
    deleteUserById: vi.fn(),
  }
})

const mockUsers: PageResult<UserVO> = {
  records: [
    { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: null, avatar: null, status: 1, roles: ['ADMIN'] },
    { id: 2, username: 'user1', nickname: '用户1', email: 'user1@example.com', phone: null, avatar: null, status: 1, roles: [] },
  ],
  total: 2,
  size: 10,
  current: 1,
  pages: 1,
}

function renderPage() {
  return render(
    <MemoryRouter>
      <UserListPage />
    </MemoryRouter>
  )
}

describe('UserListPage', () => {
  beforeEach(() => {
    vi.mocked(userApi.getUsers).mockResolvedValue(mockUsers)
    vi.mocked(userApi.deleteUserById).mockResolvedValue(undefined)
    useAuthStore.setState({ token: 'tok', isAuthenticated: true, roles: ['ADMIN'] })
  })

  it('先显示 Loading，加载后渲染用户列表', async () => {
    renderPage()
    expect(screen.getByText('Loading...')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('user1')).toBeInTheDocument()
    })
  })

  it('管理员可见删除按钮，非管理员不可见', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())
    expect(screen.getAllByRole('button', { name: /删除/ })).toHaveLength(2)

    // 切换为非管理员
    useAuthStore.setState({ roles: [] })
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /删除/ })).not.toBeInTheDocument()
  })

  it('点击删除 → 确认弹窗 → 调用 deleteUserById', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const [firstDeleteBtn] = screen.getAllByRole('button', { name: /删除/ })
    await user.click(firstDeleteBtn)

    expect(window.confirm).toHaveBeenCalled()
    expect(userApi.deleteUserById).toHaveBeenCalledWith(1)
  })

  it('API 失败时显示错误信息', async () => {
    vi.mocked(userApi.getUsers).mockRejectedValue(new Error('服务不可用'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('服务不可用')).toBeInTheDocument()
    })
  })
})
```

- [ ] **Step 3.2：运行测试，确认全部失败（页面尚未创建）**

```bash
cd frontend && npx vitest run src/pages/UserListPage.test.tsx
```

期望：所有测试 `FAIL`（`Cannot find module './UserListPage'`）

### Step 4：实现 UserListPage

- [ ] **Step 4.1：创建 `frontend/src/pages/UserListPage.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { Trash2, ChevronLeft, ChevronRight } from 'lucide-react'
import { usePermission } from '@/hooks/usePermission'
import { getUsers, deleteUserById } from '@/api/user'
import type { UserVO, PageResult } from '@/api/user'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

const PAGE_SIZE = 10

export default function UserListPage() {
  const isAdmin = usePermission('ADMIN')
  const [data, setData] = useState<PageResult<UserVO> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  const load = (p: number) => {
    setLoading(true)
    setError(null)
    getUsers(p, PAGE_SIZE)
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load(page)
  }, [page])

  const handleDelete = (id: number) => {
    if (!confirm('确认删除该用户？')) return
    deleteUserById(id)
      .then(() => load(page))
      .catch((e: Error) => setError(e.message))
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">User Management</h1>
        <p className="text-muted-foreground text-sm font-medium mt-1">
          Manage system users and role assignments.
        </p>
      </div>

      <Card className="border-border shadow-none">
        <CardContent className="p-0">
          {loading && (
            <div className="p-8 text-center text-muted-foreground text-sm">Loading...</div>
          )}
          {error && (
            <div className="p-8 text-center text-destructive text-sm">{error}</div>
          )}
          {!loading && !error && data?.records.length === 0 && (
            <div className="p-8 text-center text-muted-foreground text-sm">No users found.</div>
          )}
          {!loading && !error && data && data.records.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {['Username', 'Nickname', 'Email', 'Status'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left font-mono text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
                      {h}
                    </th>
                  ))}
                  {isAdmin && <th className="px-4 py-3" />}
                </tr>
              </thead>
              <tbody>
                {data.records.map((user) => (
                  <tr key={user.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3 font-medium text-foreground">{user.username}</td>
                    <td className="px-4 py-3 text-muted-foreground">{user.nickname ?? '—'}</td>
                    <td className="px-4 py-3 text-muted-foreground">{user.email ?? '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                        user.status === 1 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                      }`}>
                        {user.status === 1 ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={`删除 ${user.username}`}
                          className="h-7 w-7 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                          onClick={() => handleDelete(user.id)}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      {data && data.pages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Total {data.total} users</span>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <span className="px-2 font-mono">
              Page {data.current} / {data.pages}
            </span>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              disabled={page >= data.pages}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 4.2：运行测试，确认全绿**

```bash
cd frontend && npx vitest run src/pages/UserListPage.test.tsx
```

期望：4 个测试全部 `PASS`

### Step 5：注册路由

- [ ] **Step 5.1：在 `frontend/src/router/index.tsx` 注册 /users 路由**

将文件改为：

```tsx
import { createHashRouter } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import UserListPage from '@/pages/UserListPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ForbiddenPage from '@/pages/ForbiddenPage';
import RequireAuth from '@/router/RequireAuth';

export const router = createHashRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/403',
    element: <ForbiddenPage />,
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <MainLayout />
      </RequireAuth>
    ),
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'users',
        element: (
          <RequireAuth requiredRole="ADMIN">
            <UserListPage />
          </RequireAuth>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
```

- [ ] **Step 5.2：运行全部前端测试，确认没有破坏已有测试**

```bash
cd frontend && npm run test:run
```

期望：所有测试 `PASS`（含原有 LoginPage / useAuthStore / usePermission 测试）

- [ ] **Step 5.3：提交**

```bash
git add frontend/src/api/user.ts \
        frontend/src/mocks/handlers.ts \
        frontend/src/pages/UserListPage.tsx \
        frontend/src/pages/UserListPage.test.tsx \
        frontend/src/router/index.tsx
git commit -m "feat(frontend): add UserListPage as CRUD reference with pagination and delete"
```

---

## 完成验收

运行以下命令，全部绿色即完成：

```bash
# 后端全测试
cd backend && mvn test -q

# 前端全测试
cd frontend && npm run test:run
```

完成后预估总分提升：

| 维度 | 当前 | 完成后 |
|------|------|--------|
| 功能完整性 | 72 | ~82 |
| 代码质量 | 82 | ~86 |
| 可测试性 | 68 | ~78 |
| 文档质量 | 78 | ~86 |
| **总分** | **76.4** | **~82** |
