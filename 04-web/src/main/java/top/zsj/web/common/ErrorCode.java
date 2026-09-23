package top.zsj.web.common;

import lombok.Getter;

/**
 * 统一错误码
 * <p>
 * 把「状态码 + 提示语」收在一处，避免满项目散落魔法数字。
 * 业务码从 1000 起，和 HTTP 状态码分开，前端能一眼区分
 * 「网络/协议层出错」还是「业务规则不通过」。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 错误码枚举
 */
@Getter
public enum ErrorCode {

    /** 通用成功 */
    SUCCESS(200, "操作成功"),

    /** 以下与 HTTP 状态码对齐 */
    PARAM_INVALID(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录状态已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    /** 以下为业务码 */
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    ;

    /** 业务状态码 */
    private final Integer code;

    /** 默认提示语 */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
