package com.music163.starter.role;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music163.starter.role.Role;
import com.music163.starter.role.RoleVO;

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
