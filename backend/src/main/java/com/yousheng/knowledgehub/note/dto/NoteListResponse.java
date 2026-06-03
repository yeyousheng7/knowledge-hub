package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record NoteListResponse(
        @Schema(description = "当前页数据列表")
        List<NoteListItemResponse> items,
        @Schema(description = "符合当前查询条件的总记录数")
        long total,
        @Schema(description = "当前页码")
        long page,
        @Schema(description = "每页数量")
        long size
) {
}
