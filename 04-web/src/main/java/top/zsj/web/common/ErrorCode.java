package top.zsj.web.common;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 把「状态码 + 提示语」收在一处，避免满项目散落魔法数字。
 * HTTP 对齐码（200/400/404/500）与业务码（30001 起）分开，
 * 前端能一眼区分「网络/协议层出错」还是「业务规则不通过」。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 错误码枚举
 */
@Getter
public enum ErrorCode {

    /** 成功 */
    SUCCESS(200, "success"),

    /** 以下与 HTTP 状态码对齐 */
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试");

    // 业务异常（示例：以后按模块往下加）
    // ACCOUNT_NOT_FOUND_ERROR(30001, "账号不存在"),
    // PASSWORD_ERROR(30002, "密码错误"),
    // ACCOUNT_ERROR(30003, "账号被禁用");

    /** 业务状态码 */
    private final Integer code;

    /** 默认提示语 */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
