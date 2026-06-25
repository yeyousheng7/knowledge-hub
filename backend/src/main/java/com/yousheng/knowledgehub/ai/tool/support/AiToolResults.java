package com.yousheng.knowledgehub.ai.tool.support;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;

import java.util.List;

public final class AiToolResults {

    private AiToolResults() {
    }

    public static <T> AiToolResult<T> success(T data) {
        return new AiToolResult<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMsg(), data, List.of());
    }

    public static <T> AiToolResult<T> success(T data, List<String> warnings) {
        List<String> safe = warnings == null ? List.of() : List.copyOf(warnings);
        return new AiToolResult<>(true, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMsg(), data, safe);
    }

    public static <T> AiToolResult<T> failure(ErrorCode errorCode) {
        return new AiToolResult<>(false, errorCode.getCode(), errorCode.getDefaultMsg(), null, List.of());
    }

    public static <T> AiToolResult<T> failure(ErrorCode errorCode, String message) {
        return new AiToolResult<>(false, errorCode.getCode(), message, null, List.of());
    }

    public static <T> AiToolResult<T> failure(BizException exception) {
        return new AiToolResult<>(false, exception.getErrorCode().getCode(), exception.getMessage(), null, List.of());
    }

    public static <T> AiToolResult<T> failure(ErrorCode errorCode, String message, List<String> warnings) {
        List<String> safe = warnings == null ? List.of() : List.copyOf(warnings);
        return new AiToolResult<>(false, errorCode.getCode(), message, null, safe);
    }
}
