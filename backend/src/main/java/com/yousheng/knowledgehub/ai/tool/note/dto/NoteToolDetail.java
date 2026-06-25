package com.yousheng.knowledgehub.ai.tool.note.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoteToolDetail(
        Long id,
        String title,
        String summary,
        String contentMd,
        boolean contentTruncated,
        int contentLength,
        List<NoteToolTag> tags,
        String visibility,
        String moderationStatus,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public NoteToolDetail {
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
