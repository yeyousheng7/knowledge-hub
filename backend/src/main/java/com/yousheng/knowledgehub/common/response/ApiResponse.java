package com.yousheng.knowledgehub.common.response;

import com.yousheng.knowledgehub.common.exception.ErrorCode;

public record ApiResponse<T>(
        int code,
        String msg,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        ErrorCode success = ErrorCode.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getDefaultMsg(), data);
    }

    public static <T> ApiResponse<T> ok() {
        ErrorCode success = ErrorCode.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getDefaultMsg(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getDefaultMsg(), data);
    }

    public static <T> ApiResponse<T> fail(int code, String msg, T data) {
        return new ApiResponse<>(code, msg, data);
    }

}