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
