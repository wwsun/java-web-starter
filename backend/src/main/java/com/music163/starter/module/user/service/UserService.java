package com.music163.starter.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.security.dto.RegisterRequest;

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
}
