package com.yousheng.knowledgehub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;


@RequiredArgsConstructor
@Service
public class TokenBlacklistService {
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    public void blacklist(String token) {
        Instant expiredAt = jwtTokenProvider.getExpiration(token);
        Duration ttl = Duration.between(Instant.now(), expiredAt);

        if (ttl.getSeconds() <= 0) {
            return;
        }

        String tokenSHA256 = sha256(token);
        String key = toRedisBlacklistKey(tokenSHA256);
        stringRedisTemplate.opsForValue().set(key, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        String tokenSHA256 = sha256(token);
        String key = toRedisBlacklistKey(tokenSHA256);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String toRedisBlacklistKey(String value) {
        return "auth:blacklist:" + value;
    }
}
