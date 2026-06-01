package com.yousheng.knowledgehub.note.dto;

import java.time.LocalDateTime;

public record NoteDetailResponse(
        Long id,
        String title,
        String contentMd,
        String summary,
        String visibility,
        String moderationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
}
