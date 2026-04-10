package com.music163.starter.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.music163.starter.common.TestJwtHelper.TEST_SECRET;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 3_600_000L, 604_800_000L);
    }

    @Test
    void generateAccessToken_typeShouldBeAccess() {
        String token = provider.generateAccessToken("user1");
        // validateAccessToken 返回 true 说明 type == "access"
        assertThat(provider.validateAccessToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_typeShouldBeRefresh() {
        String token = provider.generateRefreshToken("user1");
        assertThat(provider.validateRefreshToken(token)).isTrue();
    }

    @Test
    void validateAccessToken_shouldRejectRefreshToken() {
        String refreshToken = provider.generateRefreshToken("user1");
        assertThat(provider.validateAccessToken(refreshToken)).isFalse();
    }

    @Test
    void validateRefreshToken_shouldRejectAccessToken() {
        String accessToken = provider.generateAccessToken("user1");
        assertThat(provider.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    void validateAccessToken_shouldRejectExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(TEST_SECRET, -1_000L, 604_800_000L);
        String expiredToken = shortLived.generateAccessToken("user1");
        assertThat(provider.validateAccessToken(expiredToken)).isFalse();
    }

    @Test
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String token = provider.generateAccessToken("alice");
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
    }
}
