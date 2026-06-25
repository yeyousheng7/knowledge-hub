package com.yousheng.knowledgehub.ai.tool.model;

import java.util.List;

public record AiToolResult<T>(
        boolean success,
        int code,
        String message,
        T data,
        List<String> warnings
) {
    public AiToolResult {
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }
}
