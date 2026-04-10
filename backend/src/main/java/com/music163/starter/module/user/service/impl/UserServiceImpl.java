package com.music163.starter.module.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    @Override
    @Cacheable(value = "user", key = "#username", unless = "#result == null")
    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public IPage<User> pageUsers(Page<User> page) {
        return userMapper.selectPage(page, null);
    }

    @Override
    @CacheEvict(value = "user", key = "#entity.username")
    public boolean save(User entity) {
        return super.save(entity);
    }
}
