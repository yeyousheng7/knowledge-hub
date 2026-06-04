package com.yousheng.knowledgehub.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CategoryListItemResponse(
        Long id,
        @Schema(description = "分类名称")
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
