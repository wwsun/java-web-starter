package com.music163.starter.module.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.module.user.service.UserService;
import com.music163.starter.module.user.dto.ChangePasswordRequest;
import com.music163.starter.module.user.dto.UpdateUserRequest;
import com.music163.starter.module.user.vo.UserVO;
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
}
