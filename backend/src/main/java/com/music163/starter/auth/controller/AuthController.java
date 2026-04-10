package com.music163.starter.auth.controller;

import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.entity.User;
import com.music163.starter.module.user.mapper.UserMapper;
import com.music163.starter.security.JwtTokenProvider;
import com.music163.starter.security.dto.LoginRequest;
import com.music163.starter.security.dto.RegisterRequest;
import com.music163.starter.security.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 提供登录、注册、Token 刷新功能。
 */
@Slf4j
@Tag(name = "认证管理", description = "登录、注册、Token 刷新")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            return Result.success(TokenResponse.of(accessToken, refreshToken, 3600));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setDeleted(0);

        userMapper.insert(user);
        log.info("User registered: {}", request.getUsername());

        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        return Result.success(TokenResponse.of(newAccessToken, newRefreshToken, 3600));
    }
}
