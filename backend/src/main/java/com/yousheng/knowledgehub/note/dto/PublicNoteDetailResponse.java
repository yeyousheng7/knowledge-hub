package com.yousheng.knowledgehub.note.dto;

import java.time.LocalDateTime;

public record PublicNoteDetailResponse(
        Long id,
        String title,
        String contentMd,
        String summary,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
