package com.gourmet.common.result;

import lombok.Getter;

/**
 * 统一状态码。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录状态已失效"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),
    CONFLICT(409, "数据冲突，请勿重复提交"),
    PAYLOAD_TOO_LARGE(413, "上传文件过大"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
