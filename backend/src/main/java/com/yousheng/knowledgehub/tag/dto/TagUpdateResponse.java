package com.yousheng.knowledgehub.tag.dto;

import java.time.LocalDateTime;

public record TagUpdateResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
