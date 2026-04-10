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
   import client from './client'
   export function getMeetingRooms() {
     return client.get('/meeting-rooms')
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
