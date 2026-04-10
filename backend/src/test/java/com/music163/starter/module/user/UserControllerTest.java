package com.music163.starter.module.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.controller.UserController;
import com.music163.starter.module.user.dto.ChangePasswordRequest;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.module.user.service.UserService;
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
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserMapper userMapper;

    private static final String TOKEN = TestJwtHelper.accessToken(TestJwtHelper.TEST_USERNAME);
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    @BeforeEach
    void setUpSecurityMocks() {
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME).password("pass").roles("USER").build();
        given(jwtTokenProvider.validateAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(TOKEN)).willReturn(TestJwtHelper.TEST_USERNAME);
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(ud);
    }

    // ===== GET /users/me =====

    @Test
    void getCurrentUser_whenNotAuthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_whenAuthenticated_shouldReturnVOWithoutPassword() throws Exception {
        User user = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME)
                .nickname("Test").status(1).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(user);

        mockMvc.perform(get("/users/me").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TestJwtHelper.TEST_USERNAME))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    // ===== DELETE /users/{id} =====

    @Test
    void deleteUser_whenDeletingSelf_shouldReturnError() throws Exception {
        User currentUser = User.builder().id(1L).username(TestJwtHelper.TEST_USERNAME).build();
        given(userService.findByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(currentUser);

        mockMvc.perform(delete("/users/1").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不能删除当前登录用户"));
    }

    // ===== PUT /users/me/password =====

    @Test
    void changePassword_whenOldPasswordWrong_shouldReturnError() throws Exception {
        doThrow(new BusinessException(ResultCode.INVALID_CREDENTIALS))
                .when(userService).changePassword(eq(TestJwtHelper.TEST_USERNAME), any());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass123");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_CREDENTIALS.getCode()));
    }
}
