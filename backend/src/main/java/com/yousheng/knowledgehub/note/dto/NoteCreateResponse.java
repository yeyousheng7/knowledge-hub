package com.yousheng.knowledgehub.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NoteCreateResponse(
        Long id,
        String title,
        @Schema(description = "可见性，PRIVATE 表示私有，PUBLIC 表示公开")
        String visibility,
        @Schema(description = "内容治理状态，NORMAL 表示正常，TAKEN_DOWN 表示已下架")
        String moderationStatus,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
