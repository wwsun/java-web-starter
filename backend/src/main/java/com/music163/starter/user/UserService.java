package com.music163.starter.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music163.starter.user.User;
import com.music163.starter.user.dto.ChangePasswordRequest;
import com.music163.starter.user.dto.UpdateUserRequest;
import com.music163.starter.user.UserVO;
import com.music163.starter.auth.dto.RegisterRequest;

/**
 * 用户 Service 接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 分页查询用户
     */
    IPage<User> pageUsers(Page<User> page);

    /**
     * 注册新用户
     *
     * @throws com.music163.starter.common.exception.BusinessException 用户名已存在时
     */
    void register(RegisterRequest request);

    /**
     * 修改密码
     *
     * @throws com.music163.starter.common.exception.BusinessException 旧密码错误或用户不存在时
     */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * 更新用户信息（仅更新非空字段）
     */
    UserVO updateUserInfo(String username, UpdateUserRequest request);

    /**
     * 删除用户并清除缓存
     *
     * @param id              目标用户 ID
     * @param currentUsername 当前登录用户名，用于防止自删
     * @throws com.music163.starter.common.exception.BusinessException 用户不存在或尝试删除自身时
     */
    void deleteUser(Long id, String currentUsername);
}
