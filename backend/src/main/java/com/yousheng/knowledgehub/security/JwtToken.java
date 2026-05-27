package com.yousheng.knowledgehub.security;

public record JwtToken(
        String accessToken,
        long expiresIn
) {
}
