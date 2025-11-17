package org.tbox.base.core.enums;

/**
 * 系统标准错误码枚举
 */
public enum StandardErrorCodeEnum {

    SYSTEM_ERROR("SYS-00001", "系统错误"),
    PARAMETER_ERROR("SYS-00002", "参数错误"),
    UNAUTHORIZED("SYS-00003", "未授权访问"),
    FORBIDDEN("SYS-00004", "禁止访问"),
    NOT_FOUND("SYS-00005", "资源不存在"),
    TIMEOUT("SYS-00006", "操作超时"),
    CONCURRENT_ERROR("SYS-00007", "并发操作错误"),
    DATA_INTEGRITY_ERROR("SYS-00008", "数据完整性错误"),
    VALIDATION_ERROR("SYS-00009", "数据验证错误"),
    SERVICE_UNAVAILABLE("SYS-00010", "服务不可用"),
    BIZ_ERROR("SYS-00011", "业务错误"),
    REPEAT_CONSUMER_ERROR("SYS-00012", "重复消费");

    private final String code;

    private final String message;

    StandardErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
