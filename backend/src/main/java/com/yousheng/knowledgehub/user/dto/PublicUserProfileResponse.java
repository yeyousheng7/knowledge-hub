package com.yousheng.knowledgehub.user.dto;

import java.time.LocalDateTime;

public record PublicUserProfileResponse(
        String username,
        String nickname,
        String bio,
        LocalDateTime createdAt
) {
}
