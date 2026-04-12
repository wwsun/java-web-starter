package com.music163.starter.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.role.RoleService;
import com.music163.starter.user.UserController;
import com.music163.starter.user.dto.ChangePasswordRequest;
import com.music163.starter.user.User;
import com.music163.starter.user.UserMapper;
import com.music163.starter.role.RoleMapper;
import com.music163.starter.user.UserService;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleMapper roleMapper;

    private static final String TOKEN = TestJwtHelper.accessToken(TestJwtHelper.TEST_USERNAME);
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    @BeforeEach
    void setUpSecurityMocks() {
        // 默认给测试用户赋予 ADMIN 角色（deleteUser 需要）
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("ADMIN", "USER")
                .build();
        given(jwtTokenProvider.validateAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(TOKEN)).willReturn(TestJwtHelper.TEST_USERNAME);
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(ud);
    }

        // ===== GET /api/users/me =====

    @Test
    void getCurrentUser_whenNotAuthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_whenAuthenticated_shouldReturnVOWithRoles() throws Exception {
        User user = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME)
                .nickname("Test").status(1).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(user);
        given(roleService.getRoleCodesByUserId(1L)).willReturn(List.of("ADMIN"));

        mockMvc.perform(get("/api/users/me").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TestJwtHelper.TEST_USERNAME))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

        // ===== DELETE /api/users/{id} =====

    @Test
    void deleteUser_whenNotAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME))
                .willReturn(userOnly);

        mockMvc.perform(delete("/api/users/2").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_whenAdminDeletingSelf_shouldReturnError() throws Exception {
        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录用户"))
                .when(userService).deleteUser(eq(1L), eq(TestJwtHelper.TEST_USERNAME));

        mockMvc.perform(delete("/api/users/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能删除当前登录用户"));
    }

        // ===== PUT /api/users/me/password =====

    @Test
    void changePassword_whenOldPasswordWrong_shouldReturnError() throws Exception {
        doThrow(new BusinessException(ResultCode.INVALID_CREDENTIALS))
                .when(userService).changePassword(eq(TestJwtHelper.TEST_USERNAME), any());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass123");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_CREDENTIALS.getCode()));
    }
}
