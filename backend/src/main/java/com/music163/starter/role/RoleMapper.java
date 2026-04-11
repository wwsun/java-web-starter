package com.music163.starter.role;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music163.starter.role.Role;
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
