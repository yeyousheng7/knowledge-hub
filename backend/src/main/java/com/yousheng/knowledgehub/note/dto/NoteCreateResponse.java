package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record NoteCreateResponse(
        Long id,
        String title,
        String summary,
        @Schema(description = "可见性，PRIVATE 表示私有，PUBLIC 表示公开")
        String visibility,
        @Schema(description = "内容治理状态，NORMAL 表示正常，TAKEN_DOWN 表示已下架")
        String moderationStatus,
        @Schema(description = "分类ID，null 表示未分类")
        Long categoryId,
        @Schema(description = "标签，null 或 为空表示无标签")
        List<NoteTagResponse> tags,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
