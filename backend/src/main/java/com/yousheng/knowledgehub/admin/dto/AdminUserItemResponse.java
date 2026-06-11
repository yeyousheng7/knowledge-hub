package com.yousheng.knowledgehub.admin.dto;

import java.time.LocalDateTime;

public record AdminUserItemResponse(
        Long userId,
        String username,
        String nickname,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
