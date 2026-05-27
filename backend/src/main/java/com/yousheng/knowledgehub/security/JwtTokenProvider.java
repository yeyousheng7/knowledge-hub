package com.yousheng.knowledgehub.security;

import com.yousheng.knowledgehub.config.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

    public JwtToken generateAccessToken(Long userId, String username, String role) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + jwtProperties.getExpireSeconds() * 1000;
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(getSecretKey())
                .compact();

        return new JwtToken(token, jwtProperties.getExpireSeconds());
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
