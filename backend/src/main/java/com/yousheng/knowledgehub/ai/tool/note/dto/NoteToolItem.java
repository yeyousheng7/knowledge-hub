package com.yousheng.knowledgehub.ai.tool.note.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoteToolItem(
        Long id,
        String title,
        String summary,
        List<NoteToolTag> tags,
        String visibility,
        String moderationStatus,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public NoteToolItem {
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
