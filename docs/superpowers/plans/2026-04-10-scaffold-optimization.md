# Scaffold Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复脚手架的安全漏洞、架构规范违反，补充测试基础设施和开发文档，使其成为高质量的企业项目起点。

**Architecture:** 在现有 Spring Boot 3.2 + MyBatis-Plus + Spring Security JWT 架构上做精准修复，不引入新技术栈。核心改动为：token 类型双校验、DTO/VO 分层隔离、UserService 补全业务方法、控制器切片测试示例。

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus 3.5, JJWT 0.13, JUnit 5, Mockito, Spring Security Test

**Spec:** `docs/superpowers/specs/2026-04-10-scaffold-optimization-design.md`

---

## File Map（改动全览）

| 文件 | 类型 | 说明 |
|------|------|------|
| `src/test/resources/application.yml` | 新增 | 测试专用 JWT 配置 |
| `src/test/java/.../common/TestJwtHelper.java` | 新增 | 生成测试 token 的工具类 |
| `src/test/java/.../security/JwtTokenProviderTest.java` | 新增 | JWT 类型校验单元测试（6 case） |
| `src/test/java/.../module/user/UserServiceTest.java` | 新增 | UserService 单元测试（5 case） |
| `src/test/java/.../module/user/UserControllerTest.java` | 新增 | UserController 切片测试（4 case） |
| `src/test/java/.../auth/AuthControllerTest.java` | 新增 | AuthController 切片测试（4 case） |
| `security/JwtTokenProvider.java` | 修改 | 新增 validateAccessToken/validateRefreshToken/getAccessTokenExpiration |
| `security/JwtAuthenticationFilter.java` | 修改 | 调用 validateAccessToken |
| `auth/controller/AuthController.java` | 修改 | 依赖改 UserService，refresh 用 validateRefreshToken，去掉硬编码 3600 |
| `module/user/entity/User.java` | 修改 | 补 @Builder @NoArgsConstructor @AllArgsConstructor |
| `module/user/dto/UpdateUserRequest.java` | 新增 | 更新用户信息请求 DTO |
| `module/user/dto/ChangePasswordRequest.java` | 新增 | 修改密码请求 DTO |
| `module/user/vo/UserVO.java` | 新增 | 用户响应 VO（不含 password/deleted） |
| `module/user/service/UserService.java` | 修改 | 新增 register/changePassword/updateUserInfo 方法 |
| `module/user/service/impl/UserServiceImpl.java` | 修改 | 实现新方法，注入 PasswordEncoder，移除冗余 UserMapper 注入 |
| `module/user/controller/UserController.java` | 修改 | 返回改 VO，新增 /me 接口，删除加防自删 |
| `src/main/resources/application.yml` | 修改 | 移除 StdOutImpl SQL 日志 |
| `src/main/resources/application-dev.yml` | 修改 | 补入 StdOutImpl SQL 日志 |
| `AGENTS.md` | 修改 | 补充 DTO/VO 规范、测试规范、获取当前用户方式 |
| `doc/architecture.md` | 新增 | 脚手架架构说明文档 |

---

## Task 1：测试基础设施

**Files:**
- Create: `backend/src/test/resources/application.yml`
- Create: `backend/src/test/java/com/music163/starter/common/TestJwtHelper.java`

- [ ] **Step 1：创建测试专用配置文件**

`backend/src/test/resources/application.yml`：

```yaml
# 测试环境覆盖配置
server:
  servlet:
    context-path: /   # 测试中去掉 /api 前缀，MockMvc 路径更简洁

spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER
    driver-class-name: org.h2.Driver
    username: sa
    password:
  cache:
    type: none   # 测试中关闭 Redis 缓存，避免依赖外部服务

jwt:
  secret: test-secret-key-must-be-at-least-256-bits-long-for-hs256
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

- [ ] **Step 2：创建 TestJwtHelper**

`backend/src/test/java/com/music163/starter/common/TestJwtHelper.java`：

```java
package com.music163.starter.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 测试专用 JWT 工具类
 * <p>
 * 使用与 application.yml(test) 相同的 secret，生成可被真实 JwtTokenProvider 校验的 token。
 */
public class TestJwtHelper {

    public static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256";
    public static final String TEST_USERNAME = "testuser";

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    /** 生成有效 access token */
    public static String accessToken(String username) {
        return buildToken(username, "access", 3_600_000L);
    }

    /** 生成有效 refresh token */
    public static String refreshToken(String username) {
        return buildToken(username, "refresh", 604_800_000L);
    }

    /** 生成已过期的 access token */
    public static String expiredAccessToken(String username) {
        return buildToken(username, "access", -1_000L);
    }

    private static String buildToken(String username, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(KEY)
                .compact();
    }
}
```

- [ ] **Step 3：在 pom.xml 添加 H2 测试依赖**

在 `backend/pom.xml` 的 `<dependencies>` 中追加（放在 `spring-boot-starter-test` 之后）：

```xml
<!-- ==================== H2（测试数据库） ==================== -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4：确认编译通过**

```bash
cd backend && ./mvnw compile -q
```

期望：`BUILD SUCCESS`，无报错。

- [ ] **Step 5：提交**

```bash
git add backend/src/test/ backend/pom.xml
git commit -m "test: add test infrastructure (TestJwtHelper, test application.yml, H2 dependency)"
```

---

## Task 2：Token 类型隔离（TDD）

**Files:**
- Create: `backend/src/test/java/com/music163/starter/security/JwtTokenProviderTest.java`
- Modify: `backend/src/main/java/com/music163/starter/security/JwtTokenProvider.java`
- Modify: `backend/src/main/java/com/music163/starter/security/JwtAuthenticationFilter.java`

- [ ] **Step 1：写 6 个失败测试**

`backend/src/test/java/com/music163/starter/security/JwtTokenProviderTest.java`：

```java
package com.music163.starter.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.music163.starter.common.TestJwtHelper.TEST_SECRET;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 3_600_000L, 604_800_000L);
    }

    @Test
    void generateAccessToken_typeShouldBeAccess() {
        String token = provider.generateAccessToken("user1");
        // validateAccessToken 返回 true 说明 type == "access"
        assertThat(provider.validateAccessToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_typeShouldBeRefresh() {
        String token = provider.generateRefreshToken("user1");
        assertThat(provider.validateRefreshToken(token)).isTrue();
    }

    @Test
    void validateAccessToken_shouldRejectRefreshToken() {
        String refreshToken = provider.generateRefreshToken("user1");
        assertThat(provider.validateAccessToken(refreshToken)).isFalse();
    }

    @Test
    void validateRefreshToken_shouldRejectAccessToken() {
        String accessToken = provider.generateAccessToken("user1");
        assertThat(provider.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    void validateAccessToken_shouldRejectExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(TEST_SECRET, -1_000L, 604_800_000L);
        String expiredToken = shortLived.generateAccessToken("user1");
        assertThat(provider.validateAccessToken(expiredToken)).isFalse();
    }

    @Test
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String token = provider.generateAccessToken("alice");
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
    }
}
```

- [ ] **Step 2：运行，确认失败（validateAccessToken 方法不存在）**

```bash
cd backend && ./mvnw test -pl . -Dtest=JwtTokenProviderTest -q 2>&1 | tail -5
```

期望：编译错误 `cannot find symbol: method validateAccessToken`。

- [ ] **Step 3：修改 JwtTokenProvider，新增类型校验方法**

完整替换 `JwtTokenProvider.java` 内容：

```java
package com.music163.starter.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类
 * <p>
 * 负责 Token 的生成、解析和校验。
 * 重要：validateAccessToken 和 validateRefreshToken 严格校验 type claim，
 * 防止 refresh token 被用于 API 认证，或 access token 被用于刷新。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** 生成 Access Token（type = "access"） */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), accessTokenExpiration, "access");
    }

    /** 生成 Refresh Token（type = "refresh"） */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), refreshTokenExpiration, "refresh");
    }

    /** 根据用户名生成 Access Token */
    public String generateAccessToken(String username) {
        return generateToken(username, accessTokenExpiration, "access");
    }

    /** 根据用户名生成 Refresh Token */
    public String generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration, "refresh");
    }

    /** 从 Token 中获取用户名 */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 校验 Access Token（签名 + 未过期 + type == "access"）
     * 用于 JwtAuthenticationFilter
     */
    public boolean validateAccessToken(String token) {
        return validateTokenWithType(token, "access");
    }

    /**
     * 校验 Refresh Token（签名 + 未过期 + type == "refresh"）
     * 用于 /auth/refresh 接口
     */
    public boolean validateRefreshToken(String token) {
        return validateTokenWithType(token, "refresh");
    }

    /** 返回 access token 过期时间（毫秒），供 TokenResponse 计算 expiresIn 用 */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    // ==================== Private ====================

    private String generateToken(String subject, long expirationMs, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    private boolean validateTokenWithType(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get("type", String.class));
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 4：运行测试，确认 6 个全通过**

```bash
cd backend && ./mvnw test -Dtest=JwtTokenProviderTest -q
```

期望：`Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5：更新 JwtAuthenticationFilter，改用 validateAccessToken**

修改 `JwtAuthenticationFilter.java` 第 38 行：

```java
// 改前
if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

// 改后
if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
```

- [ ] **Step 6：更新 AuthController.refresh()，改用 validateRefreshToken，去掉硬编码 3600**

修改 `AuthController.java` 的 refresh 方法和两处 TokenResponse.of() 调用：

```java
@Operation(summary = "刷新 Token")
@PostMapping("/refresh")
public Result<TokenResponse> refresh(@RequestHeader("Authorization") String bearerToken) {
    String token = bearerToken.replace("Bearer ", "");

    if (!jwtTokenProvider.validateRefreshToken(token)) {
        throw new BusinessException(ResultCode.TOKEN_INVALID);
    }

    String username = jwtTokenProvider.getUsernameFromToken(token);
    String newAccessToken = jwtTokenProvider.generateAccessToken(username);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
    long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

    return Result.success(TokenResponse.of(newAccessToken, newRefreshToken, expiresIn));
}
```

同时修改 login 方法中的 TokenResponse.of()：

```java
// 改前
return Result.success(TokenResponse.of(accessToken, refreshToken, 3600));

// 改后
return Result.success(TokenResponse.of(accessToken, refreshToken,
        jwtTokenProvider.getAccessTokenExpiration() / 1000));
```

- [ ] **Step 7：编译确认**

```bash
cd backend && ./mvnw compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 8：提交**

```bash
git add backend/src/main/java/com/music163/starter/security/ \
        backend/src/main/java/com/music163/starter/auth/ \
        backend/src/test/java/com/music163/starter/security/
git commit -m "fix(security): validate token type in filter and refresh endpoint, remove hardcoded expiresIn"
```

---

## Task 3：UserVO + 密码字段隔离（TDD）

**Files:**
- Create: `backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java`（toVO 测试，后续任务继续追加）
- Create: `backend/src/main/java/com/music163/starter/module/user/vo/UserVO.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/controller/UserController.java`

- [ ] **Step 1：写 toVO 失败测试**

`backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java`：

```java
package com.music163.starter.module.user;

import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.vo.UserVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest {

    // ===== toVO =====

    @Test
    void toVO_shouldMapFieldsAndExcludePassword() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-password")   // 此字段不应出现在 VO 中
                .nickname("Test User")
                .email("test@example.com")
                .phone("13800138000")
                .status(1)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        UserVO vo = UserVO.from(user);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("testuser");
        assertThat(vo.getNickname()).isEqualTo("Test User");
        assertThat(vo.getEmail()).isEqualTo("test@example.com");
        assertThat(vo.getPhone()).isEqualTo("13800138000");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        // UserVO 无 getPassword() 方法：编译期保证密码不泄露
    }
}
```

- [ ] **Step 2：运行确认失败（User.builder() 和 UserVO 不存在）**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest#toVO_shouldMapFieldsAndExcludePassword -q 2>&1 | tail -5
```

期望：编译错误。

- [ ] **Step 3：修改 User.java，补 @Builder/@NoArgsConstructor/@AllArgsConstructor**

`backend/src/main/java/com/music163/starter/module/user/entity/User.java`：

```java
package com.music163.starter.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @Builder.Default
    private Integer deleted = 0;
}
```

- [ ] **Step 4：创建 UserVO.java**

`backend/src/main/java/com/music163/starter/module/user/vo/UserVO.java`：

```java
package com.music163.starter.module.user.vo;

import com.music163.starter.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应 VO
 * <p>
 * 对外暴露的用户信息，不含 password、deleted 等敏感/内部字段。
 * 使用 {@link #from(User)} 从实体转换，避免在 Controller/Service 中手动 set 字段。
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
     * 从 User 实体转换为 VO（标准转换入口）
     */
    public static UserVO from(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 5：运行 toVO 测试，确认通过**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest#toVO_shouldMapFieldsAndExcludePassword -q
```

期望：`Tests run: 1, Failures: 0`

- [ ] **Step 6：更新 UserController，列表和详情接口改返回 UserVO**

修改 `UserController.java`：

```java
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
```

同时在文件头添加缺失的 import：
```java
import com.music163.starter.module.user.vo.UserVO;
```

- [ ] **Step 7：编译确认**

```bash
cd backend && ./mvnw compile -q
```

期望：`BUILD SUCCESS`

- [ ] **Step 8：提交**

```bash
git add backend/src/main/java/com/music163/starter/module/user/ \
        backend/src/test/java/com/music163/starter/module/user/
git commit -m "feat(user): add UserVO to isolate password field, fix User entity annotations"
```

---

## Task 4：AuthController 分层修复 + UserService.register（TDD）

**Files:**
- Modify: `backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java`（追加 2 个 register 测试）
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/UserService.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/impl/UserServiceImpl.java`
- Modify: `backend/src/main/java/com/music163/starter/auth/controller/AuthController.java`

- [ ] **Step 1：追加 register 失败测试到 UserServiceTest.java**

在 `UserServiceTest.java` 中追加（类级别添加字段和测试方法）：

```java
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.security.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// 在类声明上添加
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(passwordEncoder);
        // 注入 baseMapper（ServiceImpl 的受保护字段）
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    // ... 保留已有 toVO 测试（移除 @BeforeEach 前的 User.builder 调用，改从 setUp 外直接使用）

    // ===== register =====

    @Test
    void register_success_shouldEncodePassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        given(userMapper.selectByUsername("newuser")).willReturn(null);
        given(passwordEncoder.encode("password123")).willReturn("hashed");
        given(userMapper.insert(any(User.class))).willReturn(1);

        userService.register(request);

        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(argThat(u -> "hashed".equals(u.getPassword())));
    }

    @Test
    void register_duplicateUsername_shouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        given(userMapper.selectByUsername("existing")).willReturn(new User());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class);
    }
}
```

> 注意：`toVO` 测试需要保留，但由于 `setUp()` 中初始化了 `userService`，`toVO_shouldMapFieldsAndExcludePassword` 方法体保持不变（它是静态方法 `UserVO.from()`，不依赖 userService）。

- [ ] **Step 2：运行确认失败（register 方法不存在）**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest -q 2>&1 | tail -5
```

期望：编译错误 `cannot find symbol: method register`。

- [ ] **Step 3：UserService 接口新增 register 方法**

`UserService.java` 追加方法声明：

```java
import com.music163.starter.security.dto.RegisterRequest;

/**
 * 注册新用户
 *
 * @throws BusinessException 用户名已存在时
 */
void register(RegisterRequest request);
```

- [ ] **Step 4：UserServiceImpl 实现 register，重构 UserMapper 注入**

完整替换 `UserServiceImpl.java`：

```java
package com.music163.starter.module.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 注意：不重复注入 UserMapper，通过 ServiceImpl 的 baseMapper 访问
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = "user", key = "#username", unless = "#result == null")
    public User findByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public IPage<User> pageUsers(Page<User> page) {
        return baseMapper.selectPage(page, null);
    }

    @Override
    public void register(RegisterRequest request) {
        if (baseMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .status(1)
                .build();
        save(user);
        log.info("User registered: {}", request.getUsername());
    }

    @Override
    @CacheEvict(value = "user", key = "#entity.username")
    public boolean save(User entity) {
        return super.save(entity);
    }
}
```

- [ ] **Step 5：运行 register 测试，确认通过**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest -q
```

期望：`Tests run: 3, Failures: 0`（toVO + 2 个 register）

- [ ] **Step 6：重构 AuthController，改依赖 UserService**

完整替换 `AuthController.java`：

```java
package com.music163.starter.auth.controller;

import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.dto.LoginRequest;
import com.music163.starter.security.dto.RegisterRequest;
import com.music163.starter.security.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 提供登录、注册、Token 刷新功能。
 * 注册逻辑委托给 UserService，保持 Controller 职责单一。
 */
@Slf4j
@Tag(name = "认证管理", description = "登录、注册、Token 刷新")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;
            return Result.success(TokenResponse.of(accessToken, refreshToken, expiresIn));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        if (!jwtTokenProvider.validateRefreshToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;
        return Result.success(TokenResponse.of(newAccessToken, newRefreshToken, expiresIn));
    }
}
```

- [ ] **Step 7：编译并运行所有测试**

```bash
cd backend && ./mvnw test -q
```

期望：`BUILD SUCCESS`，所有测试通过。

- [ ] **Step 8：提交**

```bash
git add backend/src/main/java/com/music163/starter/
git add backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java
git commit -m "refactor(auth): move register logic to UserService, fix AuthController layering"
```

---

## Task 5：changePassword（TDD）

**Files:**
- Create: `backend/src/main/java/com/music163/starter/module/user/dto/ChangePasswordRequest.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/UserService.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/impl/UserServiceImpl.java`
- Modify: `backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java`（追加 2 个 case）

- [ ] **Step 1：追加 changePassword 和 findByUsername 测试**

在 `UserServiceTest.java` 已有测试后追加：

```java
import com.music163.starter.module.user.dto.ChangePasswordRequest;

// ===== findByUsername =====

@Test
void findByUsername_notFound_shouldReturnNull() {
    given(userMapper.selectByUsername("unknown")).willReturn(null);

    User result = userService.findByUsername("unknown");

    assertThat(result).isNull();
}

// ===== changePassword =====

@Test
void changePassword_wrongOldPassword_shouldThrowBusinessException() {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setOldPassword("wrong-password");
    request.setNewPassword("newpass123");

    User user = User.builder()
            .username("testuser")
            .password("encoded-correct-password")
            .build();
    given(userMapper.selectByUsername("testuser")).willReturn(user);
    given(passwordEncoder.matches("wrong-password", "encoded-correct-password")).willReturn(false);

    assertThatThrownBy(() -> userService.changePassword("testuser", request))
            .isInstanceOf(BusinessException.class);
}
```

- [ ] **Step 2：运行，确认失败（ChangePasswordRequest 和 changePassword 不存在）**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest -q 2>&1 | tail -5
```

期望：编译错误。

- [ ] **Step 3：创建 ChangePasswordRequest.java**

`backend/src/main/java/com/music163/starter/module/user/dto/ChangePasswordRequest.java`：

```java
package com.music163.starter.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需在 6-32 位之间")
    private String newPassword;
}
```

- [ ] **Step 4：UserService 接口新增 changePassword**

```java
import com.music163.starter.module.user.dto.ChangePasswordRequest;

/**
 * 修改密码
 *
 * @throws BusinessException 旧密码错误或用户不存在时
 */
void changePassword(String username, ChangePasswordRequest request);
```

- [ ] **Step 5：UserServiceImpl 实现 changePassword**

在 `UserServiceImpl` 类中追加方法：

```java
@Override
@CacheEvict(value = "user", key = "#username")
public void changePassword(String username, ChangePasswordRequest request) {
    User user = findByUsername(username);
    if (user == null) {
        throw new BusinessException(ResultCode.USER_NOT_FOUND);
    }
    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
        throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
    }
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    updateById(user);
}
```

- [ ] **Step 6：运行所有 UserService 测试，确认 5 个全通过**

```bash
cd backend && ./mvnw test -Dtest=UserServiceTest -q
```

期望：`Tests run: 5, Failures: 0`

- [ ] **Step 7：提交**

```bash
git add backend/src/main/java/com/music163/starter/module/user/
git add backend/src/test/java/com/music163/starter/module/user/UserServiceTest.java
git commit -m "feat(user): add changePassword to UserService with unit tests"
```

---

## Task 6：/users/me 接口 + 防自删保护（TDD）

**Files:**
- Create: `backend/src/test/java/com/music163/starter/module/user/UserControllerTest.java`
- Create: `backend/src/main/java/com/music163/starter/module/user/dto/UpdateUserRequest.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/UserService.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/service/impl/UserServiceImpl.java`
- Modify: `backend/src/main/java/com/music163/starter/module/user/controller/UserController.java`

- [ ] **Step 1：写 4 个失败的 Controller 测试**

`backend/src/test/java/com/music163/starter/module/user/UserControllerTest.java`：

```java
package com.music163.starter.module.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.dto.ChangePasswordRequest;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.module.user.vo.UserVO;
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
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static final String TOKEN = TestJwtHelper.accessToken(TestJwtHelper.TEST_USERNAME);
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    @BeforeEach
    void setUpSecurityMocks() {
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME).password("pass").roles("USER").build();
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
    void getCurrentUser_whenAuthenticated_shouldReturnVOWithoutPassword() throws Exception {
        User user = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME)
                .nickname("Test").status(1).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(user);

        mockMvc.perform(get("/users/me").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TestJwtHelper.TEST_USERNAME))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // ===== DELETE /users/{id} =====

    @Test
    void deleteUser_whenDeletingSelf_shouldReturnError() throws Exception {
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

- [ ] **Step 2：运行，确认失败（接口和 DTO 不存在）**

```bash
cd backend && ./mvnw test -Dtest=UserControllerTest -q 2>&1 | tail -5
```

期望：编译错误。

- [ ] **Step 3：创建 UpdateUserRequest.java**

`backend/src/main/java/com/music163/starter/module/user/dto/UpdateUserRequest.java`：

```java
package com.music163.starter.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求
 * <p>
 * 所有字段均为可选，传入非空值时才更新对应字段。
 */
@Data
public class UpdateUserRequest {

    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;
}
```

- [ ] **Step 4：UserService 接口新增 updateUserInfo**

```java
import com.music163.starter.module.user.dto.UpdateUserRequest;
import com.music163.starter.module.user.vo.UserVO;

/**
 * 更新用户信息（仅更新非空字段）
 */
UserVO updateUserInfo(String username, UpdateUserRequest request);
```

- [ ] **Step 5：UserServiceImpl 实现 updateUserInfo**

```java
@Override
@CacheEvict(value = "user", key = "#username")
public UserVO updateUserInfo(String username, UpdateUserRequest request) {
    User user = findByUsername(username);
    if (user == null) {
        throw new BusinessException(ResultCode.USER_NOT_FOUND);
    }
    if (org.springframework.util.StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname());
    }
    if (org.springframework.util.StringUtils.hasText(request.getEmail())) {
        user.setEmail(request.getEmail());
    }
    if (org.springframework.util.StringUtils.hasText(request.getPhone())) {
        user.setPhone(request.getPhone());
    }
    if (org.springframework.util.StringUtils.hasText(request.getAvatar())) {
        user.setAvatar(request.getAvatar());
    }
    updateById(user);
    return UserVO.from(user);
}
```

- [ ] **Step 6：完整替换 UserController.java，新增 /me 接口和防自删**

```java
package com.music163.starter.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 * <p>
 * /users/me 系列接口展示了"获取当前登录用户"的标准做法：
 * 从 SecurityContextHolder 取用户名，不依赖请求参数。
 */
@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        String username = currentUsername();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(UserVO.from(user));
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

    @Operation(summary = "删除用户")
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

    /**
     * 从 Spring Security 上下文获取当前登录用户名
     * （这是脚手架中获取当前用户的标准方式）
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

- [ ] **Step 7：运行 UserControllerTest，确认 4 个全通过**

```bash
cd backend && ./mvnw test -Dtest=UserControllerTest -q
```

期望：`Tests run: 4, Failures: 0`

- [ ] **Step 8：运行全量测试**

```bash
cd backend && ./mvnw test -q
```

期望：`BUILD SUCCESS`，所有测试通过。

- [ ] **Step 9：提交**

```bash
git add backend/src/main/java/com/music163/starter/module/user/
git add backend/src/test/java/com/music163/starter/module/user/UserControllerTest.java
git commit -m "feat(user): add /users/me endpoints, self-delete protection, updateUserInfo"
```

---

## Task 7：AuthController 切片测试

**Files:**
- Create: `backend/src/test/java/com/music163/starter/auth/AuthControllerTest.java`

- [ ] **Step 1：写 4 个 AuthController 测试**

`backend/src/test/java/com/music163/starter/auth/AuthControllerTest.java`：

```java
package com.music163.starter.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.auth.controller.AuthController;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
import com.music163.starter.security.dto.LoginRequest;
import com.music163.starter.security.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void login_success_shouldReturnAccessAndRefreshToken() throws Exception {
        Authentication auth = mock(Authentication.class);
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtTokenProvider.generateAccessToken(auth)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(auth)).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3_600_000L);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    void login_wrongPassword_shouldReturnInvalidCredentials() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    void register_duplicateUsername_shouldReturnUserAlreadyExists() throws Exception {
        doThrow(new BusinessException(ResultCode.USER_ALREADY_EXISTS))
                .when(userService).register(any());

        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.USER_ALREADY_EXISTS.getCode()));
    }

    @Test
    void refresh_withAccessToken_shouldReturnTokenInvalid() throws Exception {
        given(jwtTokenProvider.validateRefreshToken("access-token")).willReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(jsonPath("$.code").value(ResultCode.TOKEN_INVALID.getCode()));
    }
}
```

- [ ] **Step 2：运行，确认 4 个测试全通过**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerTest -q
```

期望：`Tests run: 4, Failures: 0`

- [ ] **Step 3：运行全量测试，确认总计 15 个测试全通过**

```bash
cd backend && ./mvnw test -q
```

期望：`Tests run: 15, Failures: 0`（JwtTokenProvider:6 + UserService:5 + UserController:4 + AuthController:4）

- [ ] **Step 4：提交**

```bash
git add backend/src/test/java/com/music163/starter/auth/
git commit -m "test(auth): add AuthController slice tests (login/register/refresh)"
```

---

## Task 8：配置修复（SQL 日志）

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-dev.yml`

- [ ] **Step 1：从 application.yml 移除 SQL 日志配置**

删除 `application.yml` 中以下行：

```yaml
# 删除此行：
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

修改后 `mybatis-plus.configuration` 块只保留：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- [ ] **Step 2：将 SQL 日志配置移到 application-dev.yml**

在 `application-dev.yml` 末尾追加：

```yaml
# ====================================
# MyBatis-Plus（开发环境 SQL 日志）
# ====================================
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 3：编译并运行全量测试，确认无回归**

```bash
cd backend && ./mvnw test -q
```

期望：`Tests run: 15, Failures: 0`

- [ ] **Step 4：提交**

```bash
git add backend/src/main/resources/
git commit -m "fix(config): move SQL logging to dev profile only"
```

---

## Task 9：文档完善

**Files:**
- Modify: `AGENTS.md`
- Create: `doc/architecture.md`

- [ ] **Step 1：在 AGENTS.md 的"代码规范 - 后端"章节补充三块内容**

在 `AGENTS.md` 后端规范第 6 条之后追加：

```markdown
7. **DTO / VO 规范**：
   - 请求入参使用 `XxxRequest`，放在 `module/<name>/dto/`，添加 `@NotNull`/`@Size` 等校验注解
   - 响应出参使用 `XxxVO`，放在 `module/<name>/vo/`，不包含 `password`、`deleted` 等敏感字段
   - Entity 不得直接作为 Controller 返回值
   - VO 转换使用 `XxxVO.from(Entity)` 静态工厂方法，放在 VO 类中

8. **获取当前登录用户（标准方式）**：
   ```java
   String username = SecurityContextHolder.getContext().getAuthentication().getName();
   ```
   不要在 Controller 方法参数中手动解析 `Authorization` Header。

9. **测试规范**：
   - Service 层业务逻辑必须有对应单元测试（`@ExtendWith(MockitoExtension.class)`，不依赖 Spring 容器）
   - Controller 层使用 `@WebMvcTest` + `@Import(SecurityConfig.class)` 做切片测试
   - 测试用 Token 统一通过 `TestJwtHelper` 生成，不在测试中硬编码 JWT 字符串
   - 测试类命名：`<被测类名>Test.java`
```

- [ ] **Step 2：创建 doc/architecture.md**

`doc/architecture.md`：

```markdown
# 脚手架架构说明

本文档面向使用此脚手架开发新项目的工程师，说明关键技术决策、分层规范和扩展方式。

---

## 定位与目标

java-web-starter 是一套**企业内部管理系统**的前后端分离脚手架，通过 fork/clone 快速启动新项目。
内置用户管理和认证能力，User 模块作为完整的架构示范，供新模块参考复制。

---

## 技术选型决策

| 选型 | 理由 |
|------|------|
| **MyBatis-Plus** 而非 JPA | 企业内部系统 SQL 通常需要精细控制；MyBatis-Plus 在保留 SQL 透明度的同时提供 CRUD 减负 |
| **JJWT + 双令牌方案** | Access Token 短期（1h）+ Refresh Token 长期（7d），无状态设计；JJWT 是 Java 生态最广泛的 JWT 库 |
| **Knife4j** 而非 Swagger UI | Knife4j 在 SpringDoc 基础上提供更好的中文界面和调试体验，适合内部系统 |
| **Redis** 作为缓存层 | 通过 Spring Cache 抽象接入，`@Cacheable`/`@CacheEvict` 注解驱动，方便替换 |

---

## 分层架构

```
HTTP 请求
   ↓
Controller 层    职责：参数校验（@Valid）、调用 Service、返回 Result<XxxVO>
                 禁止：直接访问 Mapper、包含业务逻辑、返回 Entity
   ↓
Service 层       职责：业务逻辑、事务、缓存、异常抛出
                 禁止：直接构造 HTTP 响应、感知 SecurityContext（除获取用户名外）
   ↓
Mapper 层        职责：数据库访问，继承 BaseMapper<Entity>
                 禁止：业务逻辑

跨层禁止调用：Controller → Mapper（绕过 Service）
```

---

## 认证机制（双令牌）

```
登录 POST /auth/login
  → Spring Security 验证用户名密码
  → 生成 access_token（type=access，1h）+ refresh_token（type=refresh，7d）
  → 返回 TokenResponse

每次 API 请求
  → JwtAuthenticationFilter 提取 Bearer Token
  → validateAccessToken()：校验签名 + 未过期 + type == "access"
  → 设置 SecurityContext

Token 刷新 POST /auth/refresh（携带 refresh_token）
  → validateRefreshToken()：校验签名 + 未过期 + type == "refresh"
  → 生成新的 access_token + refresh_token
```

**type claim 校验是安全关键**：access token 不能用于刷新，refresh token 不能用于 API 认证。

---

## DTO / VO 规范

| 类型 | 用途 | 位置 |
|------|------|------|
| `XxxRequest` | 请求入参（@Valid 校验） | `module/<name>/dto/` |
| `XxxVO` | 响应出参（安全字段子集） | `module/<name>/vo/` |
| `Entity` | 数据库映射 | `module/<name>/entity/` |

转换入口：`XxxVO.from(Entity)` 静态方法（见 `UserVO.java`）

---

## 错误处理规范

1. 业务异常抛 `BusinessException(ResultCode.XXX)` 或 `BusinessException(ResultCode.XXX, "自定义消息")`
2. `GlobalExceptionHandler` 统一捕获，返回 `Result<Void>`
3. 新业务模块需要的错误码在 `ResultCode.java` 中注册（建议从 2001 开始，避免与 1xxx 用户相关码冲突）

---

## 测试策略

| 层级 | 注解 | 示例文件 |
|------|------|----------|
| 单元测试（Service/工具类） | `@ExtendWith(MockitoExtension.class)` | `UserServiceTest.java` |
| 切片测试（Controller） | `@WebMvcTest` + `@Import(SecurityConfig.class)` | `UserControllerTest.java` |
| E2E 测试（预留） | `@SpringBootTest` + H2 | 目录已创建，待扩展 |

测试用 Token 通过 `TestJwtHelper` 生成，使用与测试配置相同的 secret，可被真实 JwtTokenProvider 校验。

---

## 新增业务模块 Checklist

```
1. [ ] 在 module/<name>/ 下创建 entity/dto/vo/mapper/service/controller
2. [ ] Entity 添加 @Data @Builder @NoArgsConstructor @AllArgsConstructor
3. [ ] 请求入参定义为 XxxRequest，添加 @NotNull/@Size 等校验注解
4. [ ] 响应出参定义为 XxxVO，实现 XxxVO.from(Entity) 静态方法
5. [ ] Service 接口继承 IService<Entity>
6. [ ] Controller 统一返回 Result<T>，添加 @Tag 和 @Operation
7. [ ] 业务异常抛 BusinessException，在 ResultCode 注册错误码
8. [ ] 为 Service 核心方法编写单元测试
```
```

- [ ] **Step 3：提交**

```bash
git add AGENTS.md doc/architecture.md
git commit -m "docs: add architecture guide and update AGENTS.md with DTO/VO and test conventions"
```

---

## 最终验证

- [ ] **运行全量测试，确认全通过**

```bash
cd backend && ./mvnw test -q
```

期望：`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **编译后端**

```bash
cd backend && ./mvnw clean compile -q
```

期望：`BUILD SUCCESS`

- [ ] **确认提交历史整洁**

```bash
git log --oneline -10
```

期望看到 9 次有意义的提交，覆盖所有任务。
```
