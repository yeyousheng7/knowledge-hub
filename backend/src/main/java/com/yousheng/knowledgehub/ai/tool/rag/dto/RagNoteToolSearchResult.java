package com.yousheng.knowledgehub.ai.tool.rag.dto;

import java.util.List;

public record RagNoteToolSearchResult(
        String query,
        int topK,
        List<RagNoteToolHit> hits
) {
    public RagNoteToolSearchResult {
        hits = hits != null ? List.copyOf(hits) : List.of();
    }
}
