package com.yousheng.knowledgehub.tag.dto;

import java.time.LocalDateTime;

public record TagListItemResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}