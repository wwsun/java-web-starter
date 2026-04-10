package com.music163.starter.auth.controller;

import com.music163.starter.common.exception.BusinessException;
import com.music163.starter.common.result.Result;
import com.music163.starter.common.result.ResultCode;
import com.music163.starter.module.user.service.UserService;
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
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 提供登录、注册、Token 刷新功能。
 * 注册逻辑委托给 UserService，保持 Controller 职责单一。
 */
@Slf4j
@Tag(name = "认证管理", description = "登录、注册、Token 刷新")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;
            return Result.success(TokenResponse.of(accessToken, refreshToken, expiresIn));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        if (!jwtTokenProvider.validateRefreshToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;
        return Result.success(TokenResponse.of(newAccessToken, newRefreshToken, expiresIn));
    }
}
