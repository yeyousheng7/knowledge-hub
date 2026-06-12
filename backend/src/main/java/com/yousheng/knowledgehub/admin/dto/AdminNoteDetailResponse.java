package com.yousheng.knowledgehub.admin.dto;

import java.time.LocalDateTime;

public record AdminNoteDetailResponse(
        Long noteId,
        String title,
        String contentMd,
        String summary,
        AdminNoteAuthorResponse author,
        String visibility,
        String moderationStatus,
        LocalDateTime publishedAt,
        LocalDateTime moderatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
