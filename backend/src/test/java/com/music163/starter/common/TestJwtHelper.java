package com.music163.starter.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 测试专用 JWT 工具类
 * <p>
 * 使用与 application.yml(test) 相同的 secret，生成可被真实 JwtTokenProvider 校验的 token。
 */
public class TestJwtHelper {

    public static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256";
    public static final String TEST_USERNAME = "testuser";

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    /** 生成有效 access token */
    public static String accessToken(String username) {
        return buildToken(username, "access", 3_600_000L);
    }

    /** 生成有效 refresh token */
    public static String refreshToken(String username) {
        return buildToken(username, "refresh", 604_800_000L);
    }

    /** 生成已过期的 access token */
    public static String expiredAccessToken(String username) {
        return buildToken(username, "access", -1_000L);
    }

    private static String buildToken(String username, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(KEY)
                .compact();
    }
}
