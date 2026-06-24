package com.yousheng.knowledgehub.ai.index;

import java.util.Collections;
import java.util.List;

public record AiIndexSearchResult(
        Long userId,
        String query,
        String activeGeneration,
        List<AiIndexSearchHit> hits
) {
    public AiIndexSearchResult {
        hits = hits != null ? List.copyOf(hits) : Collections.emptyList();
    }
}
