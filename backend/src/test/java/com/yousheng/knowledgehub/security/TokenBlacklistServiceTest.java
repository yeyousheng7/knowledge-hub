package com.yousheng.knowledgehub.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    // ---- blacklist ----

    @Test
    void blacklist_validToken_setsRedisKeyWithCorrectTTL() {
        String token = "valid-token";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(jwtTokenProvider.getExpiration(token)).thenReturn(expiresAt);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist(token);

        String expectedKey = expectedKey(token);
        verify(valueOperations).set(
                eq(expectedKey),
                eq("1"),
                argThat(ttl -> ttl.getSeconds() > 0 && Math.abs(ttl.getSeconds() - 3600) <= 2)
        );
    }

    @Test
    void blacklist_expiredToken_skipsRedisWrite() {
        String token = "expired-token";
        when(jwtTokenProvider.getExpiration(token)).thenReturn(Instant.now().minusSeconds(1));

        tokenBlacklistService.blacklist(token);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void blacklist_tokenExpiringNow_skipsRedisWrite() {
        String token = "expiring-now";
        when(jwtTokenProvider.getExpiration(token)).thenReturn(Instant.now());

        tokenBlacklistService.blacklist(token);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    // ---- isBlacklisted ----

    @Test
    void isBlacklisted_existingKey_returnsTrue() {
        String token = "test-token";
        String key = expectedKey(token);
        when(stringRedisTemplate.hasKey(key)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }

    @Test
    void isBlacklisted_missingKey_returnsFalse() {
        String token = "missing-token";
        String key = expectedKey(token);
        when(stringRedisTemplate.hasKey(key)).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    void isBlacklisted_nullResult_returnsFalse() {
        String token = "null-token";
        String key = expectedKey(token);
        when(stringRedisTemplate.hasKey(key)).thenReturn(null);

        assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    // ---- helpers ----

    private static String expectedKey(String token) {
        return "auth:blacklist:" + sha256(token);
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
