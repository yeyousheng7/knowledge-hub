package com.yousheng.knowledgehub.admin.dto;

import java.time.LocalDateTime;

public record AdminNoteModerationResponse(
        Long noteId,
        String moderationStatus,
        LocalDateTime moderatedAt
) {
}
