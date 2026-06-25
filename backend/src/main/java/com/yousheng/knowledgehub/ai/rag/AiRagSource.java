package com.yousheng.knowledgehub.ai.rag;

import java.time.LocalDateTime;

public record AiRagSource(
        Long noteId,
        String title,
        String snippet,
        int chunkIndex,
        Double distance,
        String visibility,
        LocalDateTime updatedAt
) {
}
