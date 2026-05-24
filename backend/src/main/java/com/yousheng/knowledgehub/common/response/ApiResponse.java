package com.yousheng.knowledgehub.common.response;

public record ApiResponse<T>(
        int code,
        String msg,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data);
    }

    public static <T> ApiResponse<T> fail(int code, String msg, T data) {
        return new ApiResponse<>(code, msg, data);
    }

}