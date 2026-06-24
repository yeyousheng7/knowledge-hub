package com.yousheng.knowledgehub.ai.index.dto;

import java.time.Instant;

public record AiIndexRebuildResponse(
        Long userId,
        int chunkCount,
        Instant indexedAt
) {
}
