package com.fund.research.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Unified error code definitions.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "OK"),

    // Client side: 4xxxx
    BAD_REQUEST(40000, "请求参数错误"),
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    METHOD_NOT_ALLOWED(40500, "请求方法不允许"),

    // Business: 6xxxx
    BUSINESS_ERROR(60000, "业务处理失败"),
    DATA_ALREADY_EXISTS(60001, "数据已存在"),
    DATA_NOT_EXISTS(60002, "数据不存在"),

    // Server side: 5xxxx
    INTERNAL_ERROR(50000, "服务器内部错误"),
    DATABASE_ERROR(50001, "数据库异常"),
    THIRD_PARTY_ERROR(50002, "第三方服务异常"),
    DIFY_ERROR(50003, "Dify 工作流调用异常");

    private final Integer code;
    private final String message;
}
