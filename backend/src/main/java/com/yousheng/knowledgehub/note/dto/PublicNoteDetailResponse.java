package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PublicNoteDetailResponse(
        Long id,
        String title,
        @Schema(description = "Markdown 正文")
        String contentMd,
        @Schema(description = "笔记摘要")
        String summary,
        @Schema(description = "发布时间")
        LocalDateTime publishedAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
