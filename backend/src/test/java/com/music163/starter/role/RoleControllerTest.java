package com.music163.starter.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music163.starter.common.TestJwtHelper;
import com.music163.starter.role.dto.AssignRoleRequest;
import com.music163.starter.security.CustomUserDetailsService;
import com.music163.starter.security.JwtAuthenticationFilter;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.SecurityConfig;
import com.music163.starter.user.UserMapper;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void setUpAdminMocks() {
        // 默认给测试用户赋予 ADMIN 角色
        UserDetails admin = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("ADMIN", "USER")
                .build();
        given(jwtTokenProvider.validateAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(TOKEN)).willReturn(TestJwtHelper.TEST_USERNAME);
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(admin);
    }

    // ===== GET /api/roles =====

    @Test
    void listRoles_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRoles_whenNonAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(userOnly);

        mockMvc.perform(get("/api/roles").header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRoles_whenAdmin_shouldReturnRoleList() throws Exception {
        RoleVO adminRole = RoleVO.builder()
                .id(1L)
                .code("ADMIN")
                .build();
        given(roleService.listAll()).willReturn(List.of(adminRole));

        mockMvc.perform(get("/api/roles").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ===== POST /api/users/{userId}/roles =====

    @Test
    void assignRoles_whenNonAdmin_shouldReturn403() throws Exception {
        UserDetails userOnly = org.springframework.security.core.userdetails.User
                .withUsername(TestJwtHelper.TEST_USERNAME)
                .password("pass")
                .roles("USER")
                .build();
        given(userDetailsService.loadUserByUsername(TestJwtHelper.TEST_USERNAME)).willReturn(userOnly);

        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleIds(List.of(1L));

        mockMvc.perform(post("/api/users/2/roles")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignRoles_whenAdmin_shouldReturn200() throws Exception {
        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleIds(List.of(1L, 2L));

        mockMvc.perform(post("/api/users/2/roles")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
