package com.yousheng.knowledgehub.admin.dto;

public record AdminUserStatusResponse(
        Long userId,
        String username,
        String status
) {
}
