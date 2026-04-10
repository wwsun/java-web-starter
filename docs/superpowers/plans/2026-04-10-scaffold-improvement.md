# 脚手架改进 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全 RBAC 权限体系、前端测试框架（Vitest + Playwright）、安全修复及开发指引文档，使脚手架成为全栈考核的合格基线。

**Architecture:** 后端新增 `module/role/` 模块，安全层加载真实角色；前端登录后获取角色并存入 Zustand store，`usePermission` hook 控制 UI 可见性；Vitest 覆盖 store/hook/page 单元测试，Playwright 覆盖完整登录 E2E 流程。

**Tech Stack:** Java 21 / Spring Boot 3.2 / MyBatis-Plus / Spring Security (`@EnableMethodSecurity`) / React 19 / Vitest 3 / @testing-library/react / Playwright / MSW 2

---

## 文件变更汇总

| 操作 | 文件路径 |
|------|----------|
| 修改 | `doc/sql/init.sql` |
| 新建 | `backend/.../module/role/entity/Role.java` |
| 新建 | `backend/.../module/role/mapper/RoleMapper.java` |
| 新建 | `backend/.../module/role/service/RoleService.java` |
| 新建 | `backend/.../module/role/service/impl/RoleServiceImpl.java` |
| 新建 | `backend/.../module/role/dto/AssignRoleRequest.java` |
| 新建 | `backend/.../module/role/vo/RoleVO.java` |
| 新建 | `backend/.../module/role/controller/RoleController.java` |
| 修改 | `backend/.../security/CustomUserDetailsService.java` |
| 修改 | `backend/.../security/SecurityConfig.java` |
| 修改 | `backend/.../module/user/vo/UserVO.java` |
| 修改 | `backend/.../module/user/service/impl/UserServiceImpl.java` |
| 修改 | `backend/.../module/user/controller/UserController.java` |
| 修改 | `backend/.../module/user/UserControllerTest.java` |
| 修改 | `backend/src/main/resources/application-dev.yml` |
| 修改 | `backend/src/main/resources/application-prod.yml` |
| 新建 | `frontend/src/api/user.ts` |
| 修改 | `frontend/src/stores/useAuthStore.ts` |
| 新建 | `frontend/src/hooks/usePermission.ts` |
| 修改 | `frontend/src/router/index.tsx` |
| 新建 | `frontend/src/pages/ForbiddenPage.tsx` |
| 修改 | `frontend/src/pages/DashboardPage.tsx` |
| 修改 | `frontend/src/pages/LoginPage.tsx` |
| 修改 | `frontend/src/layouts/MainLayout.tsx` |
| 修改 | `frontend/src/api/client.ts` |
| 修改 | `frontend/src/mocks/handlers.ts` |
| 修改 | `frontend/vite.config.ts` |
| 修改 | `frontend/package.json` |
| 新建 | `frontend/src/test-utils/setup.ts` |
| 新建 | `frontend/src/test-utils/server.ts` |
| 新建 | `frontend/src/stores/useAuthStore.test.ts` |
| 新建 | `frontend/src/hooks/usePermission.test.ts` |
| 新建 | `frontend/src/pages/LoginPage.test.tsx` |
| 新建 | `frontend/playwright.config.ts` |
| 新建 | `frontend/e2e/auth.spec.ts` |
| 新建 | `doc/templates/module-spec-template.md` |
| 新建 | `doc/templates/api-endpoint-template.md` |
| 新建 | `doc/how-to-add-module.md` |

> **注意**：`backend/Dockerfile` 已经使用 `eclipse-temurin:21-jre-alpine`，无需修改。

> **包路径简写**：以下任务中 `...` = `com/music163/starter`

---

## Task 1: CORS 配置收紧

**Files:**
- Modify: `backend/src/main/resources/application-dev.yml`
- Modify: `backend/src/main/resources/application-prod.yml`
- Modify: `backend/src/main/java/com/music163/starter/security/SecurityConfig.java`

- [ ] **Step 1: 在 application-dev.yml 末尾追加 CORS 配置**

```yaml
# ====================================
# CORS
# ====================================
app:
  cors:
    allowed-origins: "http://localhost:5173"
```

- [ ] **Step 2: 在 application-prod.yml 末尾追加 CORS 配置**

```yaml
# ====================================
# CORS
# ====================================
app:
  cors:
    allowed-origins: "${APP_CORS_ALLOWED_ORIGINS}"
```

- [ ] **Step 3: 修改 SecurityConfig.java，注入配置值并替换通配符**

在类中增加字段（紧接在 `objectMapper` 字段后）：
```java
@Value("${app.cors.allowed-origins:http://localhost:5173}")
private String allowedOrigins;
```

将 `corsConfigurationSource()` 方法中的一行：
```java
config.setAllowedOriginPatterns(List.of("*"));
```
替换为：
```java
config.setAllowedOriginPatterns(
    java.util.Arrays.asList(allowedOrigins.split(",")));
```

在文件头部补充 import：
```java
import org.springframework.beans.factory.annotation.Value;
```

- [ ] **Step 4: 编译验证**

```bash
cd backend && ./mvnw compile -q
```
期望：BUILD SUCCESS，无报错。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/resources/application-dev.yml \
        backend/src/main/resources/application-prod.yml \
        backend/src/main/java/com/music163/starter/security/SecurityConfig.java
git commit -m "fix(security): tighten CORS allowed-origins via config"
```

---

## Task 2: RBAC SQL 表结构

**Files:**
- Modify: `doc/sql/init.sql`

- [ ] **Step 1: 在 init.sql 末尾追加建表和初始化数据**

```sql
-- -------------------------------------------
-- RBAC: roles
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
    `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(50) NOT NULL                COMMENT '角色编码，如 ADMIN / USER',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表';

-- -------------------------------------------
-- RBAC: user_roles
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表';

-- -------------------------------------------
-- Seed: roles
-- -------------------------------------------
INSERT IGNORE INTO `roles` (`code`) VALUES ('ADMIN'), ('USER');

-- -------------------------------------------
-- Seed: admin user gets ADMIN role
-- （依赖 users 表中 admin 的 id=1）
-- -------------------------------------------
INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `users` u, `roles` r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```

- [ ] **Step 2: 本地执行 SQL（需要本地 MySQL 已启动）**

```bash
docker compose up -d mysql
# 等待健康检查通过后执行：
docker compose exec mysql mysql -uroot -proot123456 starter_db \
  -e "SOURCE /docker-entrypoint-initdb.d/init.sql" 2>/dev/null || true
# 验证建表成功：
docker compose exec mysql mysql -uroot -proot123456 starter_db \
  -e "SHOW TABLES LIKE 'roles'; SHOW TABLES LIKE 'user_roles';"
```
期望：输出包含 `roles` 和 `user_roles`。

- [ ] **Step 3: 提交**

```bash
git add doc/sql/init.sql
git commit -m "feat(rbac): add roles and user_roles tables"
```

---

## Task 3: Role 后端 Entity & Mapper

**Files:**
- Create: `backend/src/main/java/com/music163/starter/module/role/entity/Role.java`
- Create: `backend/src/main/java/com/music163/starter/module/role/mapper/RoleMapper.java`

- [ ] **Step 1: 创建 Role 实体**

```java
// backend/src/main/java/com/music163/starter/module/role/entity/Role.java
package com.music163.starter.module.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 */
@Data
@TableName("roles")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码，如 ADMIN / USER
     */
    private String code;
}
```

- [ ] **Step 2: 创建 RoleMapper**

```java
// backend/src/main/java/com/music163/starter/module/role/mapper/RoleMapper.java
package com.music163.starter.module.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music163.starter.module.role.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户的所有角色编码
     */
    @Select("SELECT r.code FROM roles r " +
            "JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 查询用户的所有角色（含 id）
     */
    @Select("SELECT r.* FROM roles r " +
            "JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> selectRolesByUserId(Long userId);

    /**
     * 删除用户的所有角色关联
     */
    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    void deleteUserRoles(Long userId);

    /**
     * 为用户添加一个角色关联
     */
    @Insert("INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && ./mvnw compile -q
```
期望：BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/music163/starter/module/role/
git commit -m "feat(rbac): add Role entity and RoleMapper"
```

---

## Task 4: Role Service & Controller

**Files:**
- Create: `backend/src/main/java/com/music163/starter/module/role/service/RoleService.java`
- Create: `backend/src/main/java/com/music163/starter/module/role/service/impl/RoleServiceImpl.java`
- Create: `backend/src/main/java/com/music163/starter/module/role/dto/AssignRoleRequest.java`
- Create: `backend/src/main/java/com/music163/starter/module/role/vo/RoleVO.java`
- Create: `backend/src/main/java/com/music163/starter/module/role/controller/RoleController.java`

- [ ] **Step 1: 创建 RoleVO**

```java
// backend/src/main/java/com/music163/starter/module/role/vo/RoleVO.java
package com.music163.starter.module.role.vo;

import com.music163.starter.module.role.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleVO {

    private Long id;
    private String code;

    public static RoleVO from(Role role) {
        return RoleVO.builder()
                .id(role.getId())
                .code(role.getCode())
                .build();
    }
}
```

- [ ] **Step 2: 创建 AssignRoleRequest**

```java
// backend/src/main/java/com/music163/starter/module/role/dto/AssignRoleRequest.java
package com.music163.starter.module.role.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRoleRequest {

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;
}
```

- [ ] **Step 3: 创建 RoleService 接口**

```java
// backend/src/main/java/com/music163/starter/module/role/service/RoleService.java
package com.music163.starter.module.role.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music163.starter.module.role.entity.Role;
import com.music163.starter.module.role.vo.RoleVO;

import java.util.List;

/**
 * 角色 Service 接口
 */
public interface RoleService extends IService<Role> {

    /**
     * 获取用户的所有角色编码（如 ["ADMIN", "USER"]）
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 查询所有角色
     */
    List<RoleVO> listAll();

    /**
     * 为用户分配角色（全量替换）
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 注册时为新用户分配默认 USER 角色
     */
    void assignDefaultRole(Long userId);
}
```

- [ ] **Step 4: 创建 RoleServiceImpl**

```java
// backend/src/main/java/com/music163/starter/module/role/service/impl/RoleServiceImpl.java
package com.music163.starter.module.role.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music163.starter.module.role.entity.Role;
import com.music163.starter.module.role.mapper.RoleMapper;
import com.music163.starter.module.role.service.RoleService;
import com.music163.starter.module.role.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<RoleVO> listAll() {
        return list().stream().map(RoleVO::from).toList();
    }

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        baseMapper.deleteUserRoles(userId);
        for (Long roleId : roleIds) {
            baseMapper.insertUserRole(userId, roleId);
        }
    }

    @Override
    public void assignDefaultRole(Long userId) {
        Role userRole = lambdaQuery().eq(Role::getCode, "USER").one();
        if (userRole != null) {
            baseMapper.insertUserRole(userId, userRole.getId());
        }
    }
}
```

- [ ] **Step 5: 创建 RoleController**

```java
// backend/src/main/java/com/music163/starter/module/role/controller/RoleController.java
package com.music163.starter.module.role.controller;

import com.music163.starter.common.result.Result;
import com.music163.starter.module.role.dto.AssignRoleRequest;
import com.music163.starter.module.role.service.RoleService;
import com.music163.starter.module.role.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@Tag(name = "角色管理", description = "角色查询与用户角色分配")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "查询所有角色")
    @GetMapping("/roles")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "为用户分配角色（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        roleService.assignRolesToUser(userId, request.getRoleIds());
        return Result.success();
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
cd backend && ./mvnw compile -q
```
期望：BUILD SUCCESS。

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/music163/starter/module/role/
git commit -m "feat(rbac): add RoleService and RoleController"
```

---

## Task 5: 安全层集成 RBAC

**Files:**
- Modify: `backend/src/main/java/com/music163/starter/security/CustomUserDetailsService.java`
- Modify: `backend/src/main/java/com/music163/starter/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/vo/UserVO.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/impl/UserServiceImpl.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/controller/UserController.java`
- Modify: `backend/src/test/java/com/music163/starter/module/user/UserControllerTest.java`

- [ ] **Step 1: 更新 CustomUserDetailsService — 从 DB 加载角色**

完整替换文件内容：
```java
package com.music163.starter.security;

import com.music163.starter.module.role.mapper.RoleMapper;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义 UserDetailsService
 * <p>
 * 从数据库加载用户信息及角色，供 Spring Security 认证使用。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());

        // 兜底：未分配角色的用户默认持有 ROLE_USER
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,
                true, true, true,
                authorities
        );
    }
}
```

- [ ] **Step 2: 在 SecurityConfig 上添加 @EnableMethodSecurity**

在 `SecurityConfig.java` 的类注解处追加：
```java
@EnableMethodSecurity
```

同时在 import 区补充：
```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
```

- [ ] **Step 3: 更新 UserVO — 新增 roles 字段和重载 from 方法**

完整替换文件内容：
```java
package com.music163.starter.module.user.vo;

import com.music163.starter.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应 VO
 * <p>
 * 对外暴露的用户信息，不含 password、deleted 等敏感/内部字段。
 * 使用 {@link #from(User)} 或 {@link #from(User, List)} 从实体转换。
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;

    /**
     * 用户角色列表（角色 code，如 ["ADMIN", "USER"]）
     */
    @Builder.Default
    private List<String> roles = List.of();

    /**
     * 不含角色信息的转换（用于列表场景）
     */
    public static UserVO from(User user) {
        return from(user, List.of());
    }

    /**
     * 含角色信息的转换（用于当前用户详情场景）
     */
    public static UserVO from(User user, List<String> roles) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .build();
    }
}
```

- [ ] **Step 4: 更新 UserServiceImpl — 注册时分配默认角色**

在 `UserServiceImpl` 的依赖中注入 `RoleService`（在 `PasswordEncoder` 字段后追加）：
```java
private final RoleService roleService;
```

在 `register` 方法的 `save(user)` 调用之后追加：
```java
roleService.assignDefaultRole(user.getId());
```

同时在文件头部补充 import：
```java
import com.music163.starter.module.role.service.RoleService;
```

- [ ] **Step 5: 更新 UserController — 注入 RoleService，更新 getCurrentUser，加 @PreAuthorize**

完整替换 `UserController.java`：
```java
package com.music163.starter.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.role.service.RoleService;
import com.music163.starter.module.user.dto.ChangePasswordRequest;
import com.music163.starter.module.user.dto.UpdateUserRequest;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<IPage<UserVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        IPage<User> result = userService.pageUsers(new Page<>(page, size));
        return Result.success(result.convert(UserVO::from));
    }

    @Operation(summary = "根据 ID 查询用户")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(UserVO.from(user));
    }

    @Operation(summary = "获取当前登录用户信息（含角色）")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        String username = currentUsername();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        List<String> roles = roleService.getRoleCodesByUserId(user.getId());
        return Result.success(UserVO.from(user, roles));
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        String username = currentUsername();
        return Result.success(userService.updateUserInfo(username, request));
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = currentUsername();
        userService.changePassword(username, request);
        return Result.success();
    }

    @Operation(summary = "删除用户（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        String username = currentUsername();
        User currentUser = userService.findByUsername(username);
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录用户");
        }
        userService.removeById(id);
        return Result.success();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

- [ ] **Step 6: 更新 UserControllerTest**

完整替换文件内容：
```java
package com.music163.starter.module.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.role.service.RoleService;
import com.music163.starter.module.user.controller.UserController;
import com.music163.starter.module.user.dto.ChangePasswordRequest;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserMapper userMapper;

    private static final String TOKEN = TestJwtHelper.accessToken(TestJwtHelper.TEST_USERNAME);
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    @BeforeEach
    void setUpSecurityMocks() {
        // 默认给测试用户赋予 ADMIN 角色（deleteUser 需要）
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("ADMIN", "USER")
                .build();
        given(jwtTokenProvider.validateAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(TOKEN)).willReturn(TestJwtHelper.TEST_USERNAME);
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(ud);
    }

    // ===== GET /users/me =====

    @Test
    void getCurrentUser_whenNotAuthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_whenAuthenticated_shouldReturnVOWithRoles() throws Exception {
        User user = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME)
                .nickname("Test").status(1).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(user);
        given(roleService.getRoleCodesByUserId(1L)).willReturn(List.of("ADMIN"));

        mockMvc.perform(get("/users/me").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TestJwtHelper.TEST_USERNAME))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    // ===== DELETE /users/{id} =====

    @Test
    void deleteUser_whenNotAdmin_shouldReturn403() throws Exception {
        // 构造一个只有 USER 角色的 token 用户
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME))
                .willReturn(userOnly);

        mockMvc.perform(delete("/users/2").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_whenAdminDeletingSelf_shouldReturnError() throws Exception {
        User currentUser = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(currentUser);

        mockMvc.perform(delete("/users/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能删除当前登录用户"));
    }

    // ===== PUT /users/me/password =====

    @Test
    void changePassword_whenOldPasswordWrong_shouldReturnError() throws Exception {
        doThrow(new BusinessException(ResultCode.INVALID_CREDENTIALS))
                .when(userService).changePassword(eq(TestJwtHelper.TEST_USERNAME), any());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass123");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_CREDENTIALS.getCode()));
    }
}
```

- [ ] **Step 7: 运行所有后端测试**

```bash
cd backend && ./mvnw test -q
```
期望：BUILD SUCCESS，所有测试绿色。如出现测试失败，检查 mock 配置是否与方法签名一致。

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/music163/starter/security/CustomUserDetailsService.java \
        backend/src/main/java/com/music163/starter/security/SecurityConfig.java \
        backend/src/main/java/com/music163/starter/module/user/vo/UserVO.java \
        backend/src/main/java/com/music163/starter/module/user/service/impl/UserServiceImpl.java \
        backend/src/main/java/com/music163/starter/module/user/controller/UserController.java \
        backend/src/test/java/com/music163/starter/module/user/UserControllerTest.java
git commit -m "feat(rbac): integrate roles into security layer and UserVO"
```

---

## Task 6: RBAC 前端

**Files:**
- Create: `frontend/src/api/user.ts`
- Modify: `frontend/src/stores/useAuthStore.ts`
- Create: `frontend/src/hooks/usePermission.ts`
- Modify: `frontend/src/router/index.tsx`
- Create: `frontend/src/pages/ForbiddenPage.tsx`
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Modify: `frontend/src/pages/LoginPage.tsx`
- Modify: `frontend/src/layouts/MainLayout.tsx`
- Modify: `frontend/src/mocks/handlers.ts`

- [ ] **Step 1: 创建 user API 模块**

```typescript
// frontend/src/api/user.ts
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

/**
 * 获取当前登录用户信息（含角色）
 */
export function getMe() {
  return client.get<unknown, { data: UserVO }>('/users/me');
}
```

- [ ] **Step 2: 更新 useAuthStore — 新增 roles 字段和 setRoles**

完整替换文件内容：
```typescript
import { create } from 'zustand';

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  roles: string[];
  setTokens: (accessToken: string, refreshToken: string) => void;
  setRoles: (roles: string[]) => void;
  logout: () => void;
}

/**
 * 认证状态管理 (Zustand)
 * Token 持久化到 localStorage
 */
export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('access_token'),
  refreshToken: localStorage.getItem('refresh_token'),
  isAuthenticated: !!localStorage.getItem('access_token'),
  roles: [],

  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
    set({ token: accessToken, refreshToken, isAuthenticated: true });
  },

  setRoles: (roles: string[]) => {
    set({ roles });
  },

  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    set({ token: null, refreshToken: null, isAuthenticated: false, roles: [] });
  },
}));
```

- [ ] **Step 3: 创建 usePermission hook**

```typescript
// frontend/src/hooks/usePermission.ts
import { useAuthStore } from '@/stores/useAuthStore';

/**
 * 检查当前用户是否持有指定角色
 * 用法：const isAdmin = usePermission('ADMIN');
 */
export function usePermission(role: string): boolean {
  const roles = useAuthStore((s) => s.roles);
  return roles.includes(role);
}
```

- [ ] **Step 4: 创建 ForbiddenPage**

```typescript
// frontend/src/pages/ForbiddenPage.tsx
export default function ForbiddenPage() {
  return (
    <div className="flex flex-col items-center justify-center h-full text-center py-20">
      <p className="text-6xl font-bold text-slate-200">403</p>
      <h1 className="text-xl font-semibold text-slate-700 mt-4">无权限访问</h1>
      <p className="text-slate-500 mt-2">你没有权限查看此页面，请联系管理员。</p>
    </div>
  );
}
```

- [ ] **Step 5: 更新 router/index.tsx — RequireAuth 支持 requiredRole**

完整替换文件内容：
```typescript
import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ForbiddenPage from '@/pages/ForbiddenPage';
import { useAuthStore } from '@/stores/useAuthStore';
import type { ReactNode } from 'react';

/**
 * 路由守卫：未登录重定向到登录页；无角色权限重定向到 403 页
 */
function RequireAuth({
  children,
  requiredRole,
}: {
  children: ReactNode;
  requiredRole?: string;
}) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const roles = useAuthStore((s) => s.roles);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (requiredRole && !roles.includes(requiredRole)) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}

export const router = createBrowserRouter([
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
      // 在此添加更多业务页面路由
      // 示例：需要 ADMIN 权限的路由：
      // {
      //   path: 'admin/xxx',
      //   element: (
      //     <RequireAuth requiredRole="ADMIN">
      //       <XxxPage />
      //     </RequireAuth>
      //   ),
      // },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
```

- [ ] **Step 6: 更新 DashboardPage — 添加 ADMIN 专属入口示范**

在文件顶部追加 import（现有 `export default` 行之前）：
```typescript
import { usePermission } from '@/hooks/usePermission';
```

在 `DashboardPage` 函数体内，`const` 声明区追加：
```typescript
const isAdmin = usePermission('ADMIN');
```

在快速开始卡片数组后的 `.map(...)` 渲染块外，追加一个仅管理员可见的额外卡片：
```tsx
{isAdmin && (
  <div className="p-4 rounded-lg bg-blue-50 border border-blue-100 hover:bg-blue-100 transition-colors cursor-pointer group">
    <span className="text-2xl">👤</span>
    <h3 className="font-medium text-blue-700 mt-2">用户管理</h3>
    <p className="text-sm text-blue-500 mt-1">管理系统用户，分配角色（仅管理员可见）</p>
  </div>
)}
```

- [ ] **Step 7: 更新 LoginPage — 登录后获取角色**

在文件顶部 import 区追加：
```typescript
import { getMe } from '@/api/user';
```

从 `useAuthStore` 解构中追加 `setRoles`：
```typescript
const { setTokens, setRoles } = useAuthStore((s) => ({
  setTokens: s.setTokens,
  setRoles: s.setRoles,
}));
```

（同时删除原来的 `const setTokens = useAuthStore((s) => s.setTokens);`）

在 `handleSubmit` 的 `setTokens(...)` 调用之后、`navigate(...)` 之前插入：
```typescript
const meRes = await getMe();
setRoles(meRes.data.roles);
```

- [ ] **Step 8: 更新 MainLayout — 添加 data-testid 供 E2E 测试使用**

找到 `MainLayout.tsx` 中的退出登录按钮（`onClick={handleLogout}` 的 button），添加属性：
```tsx
data-testid="logout-btn"
```

- [ ] **Step 9: 更新 handlers.ts — 添加 /users/me mock**

在 `handlers` 数组末尾追加：
```typescript
// 当前用户信息（含角色）
http.get('/api/users/me', () => {
  return HttpResponse.json({
    code: 200,
    message: '操作成功',
    data: {
      id: 1,
      username: 'admin',
      nickname: '管理员',
      email: 'admin@example.com',
      phone: null,
      avatar: null,
      status: 1,
      roles: ['ADMIN'],
    },
  });
}),
```

- [ ] **Step 10: 启动前端验证**

```bash
cd frontend && npm run dev
```
在浏览器中用 admin/admin123 登录，验证：
- 登录后 Dashboard 显示"用户管理"管理员入口
- 登出后重定向到 `/login`

- [ ] **Step 11: 提交**

```bash
git add frontend/src/api/user.ts \
        frontend/src/stores/useAuthStore.ts \
        frontend/src/hooks/usePermission.ts \
        frontend/src/router/index.tsx \
        frontend/src/pages/ForbiddenPage.tsx \
        frontend/src/pages/DashboardPage.tsx \
        frontend/src/pages/LoginPage.tsx \
        frontend/src/layouts/MainLayout.tsx \
        frontend/src/mocks/handlers.ts
git commit -m "feat(rbac): add role-based access control on frontend"
```

---

## Task 7: Token 自动刷新

**Files:**
- Modify: `frontend/src/api/client.ts`

- [ ] **Step 1: 完整替换 client.ts**

```typescript
import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';

/**
 * Axios 实例
 * - 请求拦截器：自动注入 Access Token
 * - 响应拦截器：401 时先尝试用 Refresh Token 续期，续期失败才跳转登录
 */
const client: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 用于 token 刷新的裸 axios（不带业务拦截器，避免循环）
const bareAxios = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 刷新状态标志与等待队列
let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

// ==================== 请求拦截器 ====================
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ==================== 响应拦截器 ====================
client.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response;
    if (data.code !== undefined && data.code !== 200) {
      console.error(`[API Error] ${data.code}: ${data.message}`);
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return data;
  },
  async (error) => {
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    const rt = localStorage.getItem('refresh_token');
    if (!rt) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // 已有刷新在进行中，将当前请求加入等待队列
    if (isRefreshing) {
      return new Promise<unknown>((resolve) => {
        pendingRequests.push((newToken: string) => {
          error.config.headers.Authorization = `Bearer ${newToken}`;
          resolve(client(error.config));
        });
      });
    }

    isRefreshing = true;

    try {
      const res = await bareAxios.post('/auth/refresh', null, {
        headers: { Authorization: `Bearer ${rt}` },
      });
      const { accessToken, refreshToken: newRefreshToken } = res.data.data;
      useAuthStore.getState().setTokens(accessToken, newRefreshToken);

      // 重试等待队列中的请求
      pendingRequests.forEach((cb) => cb(accessToken));
      pendingRequests = [];

      // 重试原请求
      error.config.headers.Authorization = `Bearer ${accessToken}`;
      return client(error.config);
    } catch {
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default client;
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npm run build 2>&1 | tail -5
```
期望：build 成功，无 TypeScript 错误。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/api/client.ts
git commit -m "fix(auth): implement automatic token refresh on 401"
```

---

## Task 8: Vitest 环境搭建

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/test-utils/setup.ts`
- Create: `frontend/src/test-utils/server.ts`

- [ ] **Step 1: 安装测试依赖**

```bash
cd frontend && npm install -D \
  vitest@^3.2.4 \
  @vitest/ui@^3.2.4 \
  @testing-library/react@^16.3.0 \
  @testing-library/user-event@^14.6.1 \
  @testing-library/jest-dom@^6.6.3 \
  jsdom@^26.1.0
```

- [ ] **Step 2: 在 vite.config.ts 中追加 test 配置**

在 `export default defineConfig({` 的对象中追加 `test` 字段（与 `plugins`、`resolve`、`server` 平级）：
```typescript
test: {
  environment: 'jsdom',
  globals: true,
  setupFiles: ['./src/test-utils/setup.ts'],
},
```

同时在文件顶部追加 vitest 类型引用（第一行）：
```typescript
/// <reference types="vitest" />
```

- [ ] **Step 3: 在 package.json 的 scripts 中追加测试命令**

```json
"test": "vitest",
"test:ui": "vitest --ui",
"test:run": "vitest run"
```

- [ ] **Step 4: 创建 test-utils/setup.ts**

```typescript
// frontend/src/test-utils/setup.ts
import '@testing-library/jest-dom';
import { server } from './server';
import { beforeAll, afterAll, afterEach } from 'vitest';

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

- [ ] **Step 5: 创建 test-utils/server.ts**

```typescript
// frontend/src/test-utils/server.ts
import { setupServer } from 'msw/node';
import { handlers } from '@/mocks/handlers';

export const server = setupServer(...handlers);
```

- [ ] **Step 6: 运行验证（空测试应通过）**

```bash
cd frontend && npm run test:run 2>&1 | tail -5
```
期望：`No test files found` 或 `0 tests` — 表示环境正常，无报错。

- [ ] **Step 7: 提交**

```bash
git add frontend/package.json \
        frontend/package-lock.json \
        frontend/vite.config.ts \
        frontend/src/test-utils/
git commit -m "test(frontend): set up Vitest + MSW test environment"
```

---

## Task 9: useAuthStore 单元测试

**Files:**
- Create: `frontend/src/stores/useAuthStore.test.ts`

- [ ] **Step 1: 创建测试文件**

```typescript
// frontend/src/stores/useAuthStore.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './useAuthStore';

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    // 重置 store 状态
    useAuthStore.setState({
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      roles: [],
    });
  });

  it('初始状态：localStorage 为空时未认证', () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().roles).toEqual([]);
  });

  it('setTokens：存储 token 并标记为已认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz');

    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().token).toBe('access-abc');
    expect(localStorage.getItem('access_token')).toBe('access-abc');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-xyz');
  });

  it('logout：清除 token 和 roles，标记为未认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz');
    useAuthStore.getState().setRoles(['ADMIN']);
    useAuthStore.getState().logout();

    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().roles).toEqual([]);
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('setRoles：更新 roles 列表', () => {
    useAuthStore.getState().setRoles(['ADMIN', 'USER']);
    expect(useAuthStore.getState().roles).toEqual(['ADMIN', 'USER']);
  });
});
```

- [ ] **Step 2: 运行测试**

```bash
cd frontend && npm run test:run -- src/stores/useAuthStore.test.ts
```
期望：4 tests passed。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/stores/useAuthStore.test.ts
git commit -m "test(store): add useAuthStore unit tests"
```

---

## Task 10: usePermission 单元测试

**Files:**
- Create: `frontend/src/hooks/usePermission.test.ts`

- [ ] **Step 1: 创建测试文件**

```typescript
// frontend/src/hooks/usePermission.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePermission } from './usePermission';
import { useAuthStore } from '@/stores/useAuthStore';

describe('usePermission', () => {
  beforeEach(() => {
    useAuthStore.setState({ roles: [], isAuthenticated: false });
  });

  it('持有该角色时返回 true', () => {
    useAuthStore.setState({ roles: ['ADMIN', 'USER'] });

    const { result } = renderHook(() => usePermission('ADMIN'));

    expect(result.current).toBe(true);
  });

  it('不持有该角色时返回 false', () => {
    useAuthStore.setState({ roles: ['USER'] });

    const { result } = renderHook(() => usePermission('ADMIN'));

    expect(result.current).toBe(false);
  });

  it('未登录（roles 为空）时返回 false', () => {
    const { result } = renderHook(() => usePermission('USER'));

    expect(result.current).toBe(false);
  });
});
```

- [ ] **Step 2: 运行测试**

```bash
cd frontend && npm run test:run -- src/hooks/usePermission.test.ts
```
期望：3 tests passed。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/hooks/usePermission.test.ts
git commit -m "test(hooks): add usePermission unit tests"
```

---

## Task 11: LoginPage 组件测试

**Files:**
- Create: `frontend/src/pages/LoginPage.test.tsx`

- [ ] **Step 1: 确认 handlers.ts 包含 /users/me mock**

在 Task 6 Step 9 中已添加。如果跳过了 Task 6，需要手动在 `handlers.ts` 末尾追加：
```typescript
http.get('/api/users/me', () => {
  return HttpResponse.json({
    code: 200, message: '操作成功',
    data: { id: 1, username: 'admin', nickname: '管理员',
            email: null, phone: null, avatar: null,
            status: 1, roles: ['ADMIN'] },
  });
}),
```

- [ ] **Step 2: 创建测试文件**

```tsx
// frontend/src/pages/LoginPage.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
import { useAuthStore } from '@/stores/useAuthStore';

// Mock useNavigate，避免真实路由跳转
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    useAuthStore.setState({ token: null, isAuthenticated: false, roles: [] });
    localStorage.clear();
  });

  it('渲染用户名、密码输入框和登录按钮', () => {
    renderLoginPage();

    expect(screen.getByLabelText('用户名')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '登 录' })).toBeInTheDocument();
  });

  it('登录成功：跳转到首页', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText('用户名'), 'admin');
    await user.type(screen.getByLabelText('密码'), 'admin123');
    await user.click(screen.getByRole('button', { name: '登 录' }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });
    // 验证 roles 已写入 store
    expect(useAuthStore.getState().roles).toContain('ADMIN');
  });

  it('登录失败：显示错误提示', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText('用户名'), 'wronguser');
    await user.type(screen.getByLabelText('密码'), 'wrongpass');
    await user.click(screen.getByRole('button', { name: '登 录' }));

    await waitFor(() => {
      expect(screen.getByText('用户名或密码错误')).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 3: 运行测试**

```bash
cd frontend && npm run test:run -- src/pages/LoginPage.test.tsx
```
期望：3 tests passed。若出现 `Cannot find module 'react-router-dom'` 错误，检查 `vite.config.ts` 中的 `resolve.alias` 配置是否被 test 配置继承（vitest 默认复用 vite 的 resolve 配置，无需额外配置）。

- [ ] **Step 4: 运行全量前端测试**

```bash
cd frontend && npm run test:run
```
期望：所有测试绿色（应包含 Task 9、10、11 的测试，共 10+ tests）。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/pages/LoginPage.test.tsx
git commit -m "test(pages): add LoginPage component tests"
```

---

## Task 12: Playwright E2E 测试

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/auth.spec.ts`

- [ ] **Step 1: 安装 Playwright**

```bash
cd frontend && npm install -D @playwright/test
npx playwright install chromium
```

- [ ] **Step 2: 在 package.json 的 scripts 中追加 E2E 命令**

```json
"test:e2e": "playwright test",
"test:e2e:ui": "playwright test --ui"
```

- [ ] **Step 3: 创建 playwright.config.ts**

```typescript
// frontend/playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: 1,
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
```

- [ ] **Step 4: 创建 e2e/auth.spec.ts**

```typescript
// frontend/e2e/auth.spec.ts
import { test, expect } from '@playwright/test';

/**
 * 认证完整流程 E2E 测试
 * 前提：后端服务运行在 localhost:8080，前端 dev server 运行在 localhost:5173
 */
test.describe('认证流程', () => {
  test('未登录访问首页应重定向到登录页', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('登录成功后跳转到 Dashboard', async ({ page }) => {
    await page.goto('/login');

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('http://localhost:5173/', { timeout: 10_000 });
    await expect(page.locator('h1')).toContainText('仪表盘');
  });

  test('登出后重定向到登录页', async ({ page }) => {
    // 先登录
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/');

    // 点击退出登录
    await page.click('[data-testid="logout-btn"]');
    await expect(page).toHaveURL(/\/login/);
  });
});
```

> **注意**：E2E 测试需要后端和前端 dev server 同时运行。本地运行方式：
> 1. `docker compose up -d mysql redis`
> 2. `cd backend && ./mvnw spring-boot:run` （另一个终端）
> 3. `cd frontend && npm run dev` （另一个终端）
> 4. `cd frontend && npm run test:e2e`

- [ ] **Step 5: 提交**

```bash
git add frontend/package.json \
        frontend/package-lock.json \
        frontend/playwright.config.ts \
        frontend/e2e/
git commit -m "test(e2e): add Playwright auth flow tests"
```

---

## Task 13: Spec 模板 + Walkthrough 文档

**Files:**
- Create: `doc/templates/module-spec-template.md`
- Create: `doc/templates/api-endpoint-template.md`
- Create: `doc/how-to-add-module.md`

- [ ] **Step 1: 创建 module-spec-template.md**

```markdown
<!-- doc/templates/module-spec-template.md -->
# [模块名] 功能规格

> **使用方式**：复制此文件到项目根目录或 `doc/specs/` 中，按实际需求填写，提交前删除所有 `> 提示` 注释。

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
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE ... | 更新时间 |
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
```

- [ ] **Step 2: 创建 api-endpoint-template.md**

```markdown
<!-- doc/templates/api-endpoint-template.md -->
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
```

- [ ] **Step 3: 创建 how-to-add-module.md**

```markdown
<!-- doc/how-to-add-module.md -->
# 如何新增一个业务模块

本文以**会议室预订**为示例，演示从 Spec 到上线的完整 7 步流程。

---

## Step 1：写 Spec

复制 `doc/templates/module-spec-template.md`，保存为 `doc/specs/meeting-room-spec.md`，填写：

- 数据模型：`meeting_rooms` 表（id, name, capacity, location, status, created_at, updated_at, deleted）
- API：`GET /api/meeting-rooms`、`POST /api/meeting-rooms`、`DELETE /api/meeting-rooms/{id}`
- 待确认问题：确认后提交 Spec 再开始编码

---

## Step 2：写 SQL

在 `doc/sql/` 新增文件 `meeting_rooms.sql`：

```sql
CREATE TABLE IF NOT EXISTS `meeting_rooms` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) NOT NULL COMMENT '会议室名称',
    `capacity`   INT          NOT NULL COMMENT '容纳人数',
    `location`   VARCHAR(200) NULL     COMMENT '位置描述',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '0-不可用 1-可用',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';
```

本地手动执行（无 Flyway），生产环境由 DBA 执行。

---

## Step 3：后端开发

在 `backend/src/main/java/com/music163/starter/module/` 下创建：

```
meeting-room/
├── entity/MeetingRoom.java          # @TableName("meeting_rooms")，加 Lombok
├── mapper/MeetingRoomMapper.java    # extends BaseMapper<MeetingRoom>
├── service/MeetingRoomService.java  # extends IService<MeetingRoom>
├── service/impl/MeetingRoomServiceImpl.java
├── dto/CreateMeetingRoomRequest.java  # 请求入参，@NotBlank/@Size 校验
├── vo/MeetingRoomVO.java            # 响应出参，含 from(entity) 静态方法
└── controller/MeetingRoomController.java
```

`Controller` 示例：

```java
@Tag(name = "会议室管理")
@RestController
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @Operation(summary = "分页查询会议室")
    @GetMapping
    public Result<IPage<MeetingRoomVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(meetingRoomService.page(new Page<>(page, size))
                .convert(MeetingRoomVO::from));
    }

    @Operation(summary = "创建会议室（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<MeetingRoomVO> create(@Valid @RequestBody CreateMeetingRoomRequest req) {
        return Result.success(meetingRoomService.create(req));
    }

    @Operation(summary = "删除会议室（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        meetingRoomService.removeById(id);
        return Result.success();
    }
}
```

---

## Step 4：加权限（如需）

若新模块有仅管理员操作的接口，直接在 Controller 方法上使用 `@PreAuthorize("hasRole('ADMIN')")`。

若需要新增自定义权限点（如 `meeting-room:book`），在 `doc/sql/` 补充 INSERT 到 `roles` 对应记录（当前项目为角色级控制，无需改代码）。

---

## Step 5：前端开发

1. 在 `frontend/src/api/` 新增 `meetingRoom.ts`：
   ```typescript
   import client from './client';
   export function getMeetingRooms() {
     return client.get('/meeting-rooms');
   }
   ```

2. 在 `frontend/src/pages/` 新增 `MeetingRoomPage.tsx`

3. 在 `frontend/src/router/index.tsx` 中注册路由：
   ```tsx
   { path: 'meeting-rooms', element: <MeetingRoomPage /> }
   ```

4. 如需仅管理员可访问：
   ```tsx
   { path: 'meeting-rooms', element: (
     <RequireAuth requiredRole="ADMIN"><MeetingRoomPage /></RequireAuth>
   )}
   ```

5. 在 `frontend/src/mocks/handlers.ts` 新增 Mock 处理器（用于前端独立开发）

---

## Step 6：写测试

**后端 Service 单元测试**（`MeetingRoomServiceTest.java`）：

```java
@ExtendWith(MockitoExtension.class)
class MeetingRoomServiceTest {

    @Mock MeetingRoomMapper mapper;
    @InjectMocks MeetingRoomServiceImpl service;

    @Test
    void create_shouldSaveAndReturnVO() {
        // Arrange
        CreateMeetingRoomRequest req = new CreateMeetingRoomRequest();
        req.setName("会议室A");
        req.setCapacity(10);

        // Act & Assert — 验证无异常抛出、mapper.insert 被调用
    }
}
```

**后端 Controller 切片测试**（`MeetingRoomControllerTest.java`）：

```java
@WebMvcTest(controllers = MeetingRoomController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MeetingRoomControllerTest {
    // 参考 UserControllerTest 模式
}
```

**前端 Vitest 测试**：参考 `LoginPage.test.tsx` 和 `usePermission.test.ts` 模式。

---

## Step 7：提交前自测 Checklist

- [ ] `cd backend && ./mvnw test` — 全绿
- [ ] `cd frontend && npm run test:run` — 全绿
- [ ] 启动服务，访问 `/api/doc.html`，新接口有 `@Tag` 和 `@Operation` 描述
- [ ] VO 不含 `password`、`deleted` 字段
- [ ] 新增的 SQL 文件已提交到 `doc/sql/`
- [ ] 无硬编码 URL（前端全部通过 `client.ts` 发起）
- [ ] 无 `System.out.println`（使用 `log.info(...)`）
```

- [ ] **Step 4: 提交**

```bash
git add doc/templates/ doc/how-to-add-module.md
git commit -m "docs: add module spec templates and new module walkthrough guide"
```

---

## 最终验证

- [ ] **后端全量测试通过**

```bash
cd backend && ./mvnw test
```
期望：BUILD SUCCESS，所有测试绿色。

- [ ] **前端全量单元测试通过**

```bash
cd frontend && npm run test:run
```
期望：全部测试绿色（10+ tests）。

- [ ] **E2E 测试（需后端服务运行）**

```bash
# 确保 docker compose up -d mysql redis 已运行
# 后端和前端 dev server 已启动
cd frontend && npm run test:e2e
```
期望：3 tests passed。
