# AGENTS.md — AI Agent 行为指令

## 项目概述

这是一个前后端分离的 Web 工程脚手架（java-web-starter），用于快速搭建企业内部管理系统。

## 技术栈约束

### 后端

- **语言**: Java 21
- **框架**: Spring Boot 3.2.x
- **ORM**: MyBatis-Plus 3.5.x（禁止使用 JPA/Hibernate）
- **数据库**: MySQL 8.4
- **缓存**: Redis 7 + Spring Cache
- **认证**: Spring Security + JWT（JJWT 库）
- **API 文档**: Knife4j（SpringDoc OpenAPI）
- **构建工具**: Maven

### 前端

- **框架**: React 19 + TypeScript（严格模式）
- **构建**: Vite
- **样式**: TailwindCSS v4
- **路由**: React Router v6
- **HTTP**: Axios（封装在 src/api/client.ts）
- **状态管理**: Zustand

## 代码规范

### 后端

1. **所有实体类使用 Lombok 注解**：`@Data`, `@Builder`, `@RequiredArgsConstructor` 等
2. **Controller 层**：
   - 统一返回 `Result<T>` 包装类
   - 使用 `@Validated` 进行参数校验
   - 添加 `@Tag` 和 `@Operation` Knife4j 注解
3. **Service 层**：
   - 接口 + 实现类模式（`UserService` + `UserServiceImpl`）
   - 继承 `IService<Entity>`
4. **异常处理**：
   - 业务异常抛 `BusinessException`
   - 禁止在 Controller 层 try-catch
5. **数据库**：
   - 表名和字段名使用 snake_case
   - 逻辑删除字段统一为 `deleted`
   - 时间字段使用 `created_at` / `updated_at`

### 前端

1. **TypeScript 严格模式**：禁止使用 `any`
2. **组件命名**: PascalCase（`MeetingRoomList.tsx`）
3. **API 请求**：统一通过 `src/api/client.ts` 发起
4. **状态管理**：使用 Zustand store，文件放在 `src/stores/`
5. **样式**：使用 TailwindCSS utility classes

## 新增业务模块模板

### 后端模块

在 `backend/src/main/java/com/music163/starter/module/` 下创建：

```
module/<name>/
├── controller/<Name>Controller.java
├── service/<Name>Service.java
├── service/impl/<Name>ServiceImpl.java
├── mapper/<Name>Mapper.java
└── entity/<Name>.java
```

### 前端页面

在 `frontend/src/pages/` 下创建页面，并在 `src/router/index.tsx` 中注册路由。

## 禁止事项

- ❌ 不要修改 `common/` 下的基础组件（除非明确需要）
- ❌ 不要在前端硬编码后端 URL
- ❌ 不要绕过 Spring Security 直接处理认证
- ❌ 不要在代码中存储密钥或密码
- ❌ 不要使用 `System.out.println`，使用 SLF4J Logger
