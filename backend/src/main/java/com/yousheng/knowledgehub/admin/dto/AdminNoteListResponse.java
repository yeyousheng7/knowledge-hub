package com.yousheng.knowledgehub.admin.dto;

import java.util.List;

public record AdminNoteListResponse(
        List<AdminNoteItemResponse> items,
        long total,
        long page,
        long size
) {
}
