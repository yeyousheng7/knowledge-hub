package com.yousheng.knowledgehub.admin.dto;

import java.time.LocalDateTime;

public record AdminNoteItemResponse(
        Long noteId,
        String title,
        String summary,
        AdminNoteAuthorResponse author,
        String visibility,
        String moderationStatus,
        LocalDateTime publishedAt,
        LocalDateTime moderatedAt,
        LocalDateTime updatedAt
) {
}
