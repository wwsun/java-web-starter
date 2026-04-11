package com.music163.starter.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.auth.AuthController;
import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.user.UserMapper;
import com.music163.starter.role.RoleMapper;
import com.music163.starter.user.UserService;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
import com.music163.starter.auth.dto.LoginRequest;
import com.music163.starter.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleMapper roleMapper;

    @Test
    void login_success_shouldReturnAccessAndRefreshToken() throws Exception {
        Authentication auth = mock(Authentication.class);
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtTokenProvider.generateAccessToken(auth)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(auth)).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3_600_000L);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    void login_wrongPassword_shouldReturnInvalidCredentials() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    void register_duplicateUsername_shouldReturnUserAlreadyExists() throws Exception {
        doThrow(new BusinessException(ResultCode.USER_ALREADY_EXISTS))
                .when(userService).register(any());

        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(ResultCode.USER_ALREADY_EXISTS.getCode()));
    }

    @Test
    void refresh_withAccessToken_shouldReturnTokenInvalid() throws Exception {
        given(jwtTokenProvider.validateRefreshToken("access-token")).willReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(jsonPath("$.code").value(ResultCode.TOKEN_INVALID.getCode()));
    }
}
