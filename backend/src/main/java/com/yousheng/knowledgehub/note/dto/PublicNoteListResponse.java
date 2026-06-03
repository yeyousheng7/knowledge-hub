package com.yousheng.knowledgehub.note.dto;

import java.util.List;

public record PublicNoteListResponse(
        List<PublicNoteListItemResponse> items,
        long total,
        long page,
        long size
) {
}