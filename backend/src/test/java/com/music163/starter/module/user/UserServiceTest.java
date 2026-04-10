package com.music163.starter.module.user;

import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.vo.UserVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest {

    // ===== toVO =====

    @Test
    void toVO_shouldMapFieldsAndExcludePassword() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-password")   // 此字段不应出现在 VO 中
                .nickname("Test User")
                .email("test@example.com")
                .phone("13800138000")
                .status(1)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        UserVO vo = UserVO.from(user);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("testuser");
        assertThat(vo.getNickname()).isEqualTo("Test User");
        assertThat(vo.getEmail()).isEqualTo("test@example.com");
        assertThat(vo.getPhone()).isEqualTo("13800138000");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        // UserVO 无 getPassword() 方法：编译期保证密码不泄露
    }
}
