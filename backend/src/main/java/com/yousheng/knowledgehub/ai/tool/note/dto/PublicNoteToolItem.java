package com.yousheng.knowledgehub.ai.tool.note.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicNoteToolItem(
        Long id,
        String title,
        String summary,
        List<NoteToolTag> tags,
        NoteToolAuthor author,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public PublicNoteToolItem {
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
