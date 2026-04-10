package com.music163.starter.security;

import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义 UserDetailsService
 * <p>
 * 从数据库加载用户信息，供 Spring Security 认证使用。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,  // enabled
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                true,                    // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
