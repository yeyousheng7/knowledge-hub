package com.yousheng.knowledgehub.note.dto;

import java.util.List;

public record NoteBatchUnpublishResult(
        int affectedCount,
        List<NoteListItemResponse> affectedItems
) {
    public NoteBatchUnpublishResult {
        affectedItems = affectedItems == null ? List.of() : List.copyOf(affectedItems);
    }
}
