package top.zsj.redis.common;

import lombok.Getter;

/**
 * 错误码枚举（从 04-web 复制，按本模块业务补充）
 * <p>
 * 把「状态码 + 提示语」收在一处，避免满项目散落魔法数字。
 * HTTP 对齐码（200/400/404/500）与业务码（4xxxx 段）分开，
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
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    // ---- 手机号与验证码相关业务异常（4xxxx 段） ----
    /** 手机号格式错误 */
    PHONE_ERROR(40001, "手机号格式错误"),
    /** 手机号不存在 */
    PHONE_NOT_EXIST(40002, "手机号不存在"),
    /** 验证码为空 */
    PHONE_CODE_EMPTY(40003, "验证码不能为空"),
    /** 验证码不匹配 */
    PHONE_CODE_ERROR(40004, "验证码错误"),
    /** 验证码不存在或已过期 */
    PHONE_CODE_EXPIRED(40005, "验证码不存在或已过期"),
    /** 验证码已被使用 */
    PHONE_CODE_USED(40006, "验证码已使用"),
    /** 发送过于频繁 */
    PHONE_CODE_SEND_TOO_MANY(40007, "验证码发送过于频繁"),
    /** 短信通道发送失败 */
    PHONE_CODE_SEND_FAILED(40008, "验证码发送失败"),
    /** 当日发送次数超限 */
    PHONE_CODE_SEND_LIMIT(40009, "验证码发送次数已达上限"),
    /** 连续输错次数过多，验证码作废 */
    PHONE_CODE_ERROR_TOO_MANY(40010, "验证码错误次数过多，请重新获取"),

    // ---- 登录态 ----
    /** 登录态失效 */
    TOKEN_INVALID(40100, "登录状态已失效，请重新登录");

    /** 业务状态码 */
    private final Integer code;

    /** 默认提示语 */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
