package com.yousheng.knowledgehub.auth.dto;

public record LoginUserResponse(
        Long id,
        String username,
        String nickname,
        String role
) {
}
