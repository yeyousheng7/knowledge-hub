package com.yousheng.knowledgehub.ai.tool.rag.dto;

public record RagNoteToolHit(
        Long noteId,
        String title,
        String snippet,
        int chunkIndex,
        Double distance
) {
}
