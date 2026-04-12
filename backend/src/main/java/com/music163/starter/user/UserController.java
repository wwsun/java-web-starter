package com.music163.starter.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.role.RoleService;
import com.music163.starter.user.dto.ChangePasswordRequest;
import com.music163.starter.user.dto.UpdateUserRequest;
import com.music163.starter.user.User;
import com.music163.starter.user.UserService;
import com.music163.starter.user.UserVO;
import com.music163.starter.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
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
        String username = SecurityUtils.getCurrentUsername();
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
        String username = SecurityUtils.getCurrentUsername();
        return Result.success(userService.updateUserInfo(username, request));
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        userService.changePassword(username, request);
        return Result.success();
    }

    @Operation(summary = "删除用户（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id, SecurityUtils.getCurrentUsername());
        return Result.success();
    }
}
