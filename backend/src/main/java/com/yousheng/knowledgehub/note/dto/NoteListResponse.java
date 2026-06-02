package com.yousheng.knowledgehub.note.dto;

import java.util.List;

public record NoteListResponse(
        List<NoteListItemResponse> items,
        long total,
        long page,
        long size
) {
}
