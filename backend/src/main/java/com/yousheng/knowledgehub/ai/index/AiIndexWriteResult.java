package com.yousheng.knowledgehub.ai.index;

import java.time.Instant;

public record AiIndexWriteResult(
        Long userId,
        int chunkCount,
        Instant indexedAt
) {
}
