package com.yousheng.knowledgehub.note.dto;

import java.time.LocalDateTime;

public record NoteCreateResponse(
        Long id,
        String title,
        String visibility,
        String moderationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
