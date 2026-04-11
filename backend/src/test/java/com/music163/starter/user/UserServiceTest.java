package com.music163.starter.user;

import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.role.RoleService;
import com.music163.starter.user.User;
import com.music163.starter.user.UserMapper;
import com.music163.starter.user.UserServiceImpl;
import com.music163.starter.user.UserVO;
import com.music163.starter.user.dto.ChangePasswordRequest;
import com.music163.starter.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;

import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @Mock
    private CacheManager cacheManager;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(passwordEncoder, roleService, cacheManager);
        // 注入 baseMapper（ServiceImpl 的受保护字段）
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

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

    // ===== register =====

    @Test
    void register_success_shouldEncodePassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        given(userMapper.selectByUsername("newuser")).willReturn(null);
        given(passwordEncoder.encode("password123")).willReturn("hashed");
        given(userMapper.insert(argThat((User u) -> u != null))).willReturn(1);

        userService.register(request);

        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(argThat((User u) -> "hashed".equals(u.getPassword())));
    }

    @Test
    void register_duplicateUsername_shouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        given(userMapper.selectByUsername("existing")).willReturn(new User());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class);
    }

    // ===== findByUsername =====

    @Test
    void findByUsername_notFound_shouldReturnNull() {
        given(userMapper.selectByUsername("unknown")).willReturn(null);

        User result = userService.findByUsername("unknown");

        assertThat(result).isNull();
    }

    // ===== changePassword =====

    @Test
    void changePassword_wrongOldPassword_shouldThrowBusinessException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong-password");
        request.setNewPassword("newpass123");

        User user = User.builder()
                .username("testuser")
                .password("encoded-correct-password")
                .build();
        given(userMapper.selectByUsername("testuser")).willReturn(user);
        given(passwordEncoder.matches("wrong-password", "encoded-correct-password")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword("testuser", request))
                .isInstanceOf(BusinessException.class);
    }
}
