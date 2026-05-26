package com.yousheng.knowledgehub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS(0, "OK", HttpStatus.OK),

    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    INVALID_INVITE_CODE(40002, "邀请码错误", HttpStatus.BAD_REQUEST),
    REQUEST_BODY_INVALID(40003, "请求体格式错误", HttpStatus.BAD_REQUEST),
    REQUEST_PARAM_INVALID(40004, "请求参数类型错误", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(40100, "请先登录", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40101, "Token 无效或已过期", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(40102, "用户名或密码错误", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(40300, "无权限", HttpStatus.FORBIDDEN),
    USER_DISABLED(40301, "用户被禁用", HttpStatus.FORBIDDEN),

    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    NOTE_NOT_FOUND(40401, "笔记不存在", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(40402, "用户不存在", HttpStatus.NOT_FOUND),

    METHOD_NOT_ALLOWED(40500, "请求方法不允许", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED(41500, "不支持的媒体类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    USERNAME_EXISTS(40901, "用户名已存在", HttpStatus.CONFLICT),
    CATEGORY_NAME_EXISTS(40902, "分类名已存在", HttpStatus.CONFLICT),
    TAG_NAME_EXISTS(40903, "标签名已存在", HttpStatus.CONFLICT),

    INTERNAL_ERROR(50000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String defaultMsg;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String defaultMsg, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMsg = defaultMsg;
        this.httpStatus = httpStatus;
    }
}