package com.yousheng.knowledgehub.ai.index.model;

import java.time.LocalDateTime;

public record NoteChunk(
        String chunkId,
        Long userId,
        Long noteId,
        int chunkIndex,
        String title,
        String text,
        String visibility,
        LocalDateTime updatedAt,
        String contentHash
) {
}
