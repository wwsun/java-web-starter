package com.music163.starter.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类
 * <p>
 * 负责 Token 的生成、解析和校验。
 * 重要：validateAccessToken 和 validateRefreshToken 严格校验 type claim，
 * 防止 refresh token 被用于 API 认证，或 access token 被用于刷新。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** 生成 Access Token（type = "access"） */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), accessTokenExpiration, "access");
    }

    /** 生成 Refresh Token（type = "refresh"） */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), refreshTokenExpiration, "refresh");
    }

    /** 根据用户名生成 Access Token */
    public String generateAccessToken(String username) {
        return generateToken(username, accessTokenExpiration, "access");
    }

    /** 根据用户名生成 Refresh Token */
    public String generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration, "refresh");
    }

    /** 从 Token 中获取用户名 */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 校验 Access Token（签名 + 未过期 + type == "access"）
     * 用于 JwtAuthenticationFilter
     */
    public boolean validateAccessToken(String token) {
        return validateTokenWithType(token, "access");
    }

    /**
     * 校验 Refresh Token（签名 + 未过期 + type == "refresh"）
     * 用于 /api/auth/refresh 接口
     */
    public boolean validateRefreshToken(String token) {
        return validateTokenWithType(token, "refresh");
    }

    /** 返回 access token 过期时间（毫秒），供 TokenResponse 计算 expiresIn 用 */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    // ==================== Private ====================

    private String generateToken(String subject, long expirationMs, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    private boolean validateTokenWithType(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get("type", String.class));
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
