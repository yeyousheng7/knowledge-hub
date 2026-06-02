package com.yousheng.knowledgehub.note.dto;

import java.time.LocalDateTime;

public record NoteListItemResponse(
        Long id,
        String title,
        String summary,
        String visibility,
        String moderationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
}
