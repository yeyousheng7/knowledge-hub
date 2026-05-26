package com.yousheng.knowledgehub.auth.dto;

public record LoginResponse(
        Long id,
        String username,
        String nickname,
        String role
) {
}
