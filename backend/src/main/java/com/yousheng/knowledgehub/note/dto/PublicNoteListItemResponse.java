package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record PublicNoteListItemResponse(
        Long id,
        String title,
        @Schema(description = "笔记摘要")
        String summary,
        @Schema(description = "笔记标签")
        List<PublicNoteTagResponse> tags,
        @Schema(description = "作者信息")
        PublicNoteAuthorResponse author,
        @Schema(description = "发布时间")
        LocalDateTime publishedAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
