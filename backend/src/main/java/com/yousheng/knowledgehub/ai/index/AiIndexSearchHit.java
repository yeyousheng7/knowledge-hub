package com.yousheng.knowledgehub.ai.index;

import java.time.LocalDateTime;

public record AiIndexSearchHit(
        Long noteId,
        String title,
        String chunkText,
        int chunkIndex,
        Double distance,
        String visibility,
        LocalDateTime updatedAt
) {
}
