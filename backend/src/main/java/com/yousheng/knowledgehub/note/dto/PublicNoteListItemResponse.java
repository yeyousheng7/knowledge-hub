package com.yousheng.knowledgehub.note.dto;

import java.time.LocalDateTime;

public record PublicNoteListItemResponse(
        Long id,
        String title,
        String summary,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
