package com.yousheng.knowledgehub.security;

public record CurrentUserPrincipal(
        Long userId,
        String username,
        String role
) {
}
