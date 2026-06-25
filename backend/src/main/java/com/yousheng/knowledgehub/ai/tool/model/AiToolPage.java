package com.yousheng.knowledgehub.ai.tool.model;

import java.util.List;

public record AiToolPage<T>(
        int page,
        int size,
        int returned,
        boolean hasMore,
        List<T> items
) {
    public AiToolPage {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
