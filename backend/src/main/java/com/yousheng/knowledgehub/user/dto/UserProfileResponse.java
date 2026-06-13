package com.yousheng.knowledgehub.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String bio,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
