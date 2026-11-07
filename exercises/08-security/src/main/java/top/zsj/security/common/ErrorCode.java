package top.zsj.security.common;

import lombok.Getter;

/**
 * 错误码
 * <p>
 * 这里与常见做法不同的地方：{@code code} 直接取 HTTP 状态码，因为本模块的教学重点是
 * 「401 是没认出你是谁，403 是认出来了但不让进」，body 里的 code 与 HTTP 状态码一致，
 * 前端只看一处即可判断行为。
 *
 * @author zsj
 */
@Getter
public enum ErrorCode {

    /** 成功 */
    SUCCESS(200, "ok"),

    /** 参数错误：校验失败、JSON 解析失败 */
    BAD_REQUEST(400, "参数错误"),

    /** 未认证 / 认证已失效：没带 token、token 过期、签名对不上、被踢下线 */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /** 已认证但无权访问：角色不符、权限点不够 */
    FORBIDDEN(403, "没有访问权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 用户名或密码错误：不区分"账号不存在"与"密码错"，避免枚举账号 */
    BAD_CREDENTIALS(401, "用户名或密码错误"),

    /** 账号被禁用 */
    ACCOUNT_DISABLED(403, "账号已被禁用"),

    /** 账号被锁定 */
    ACCOUNT_LOCKED(403, "账号已被锁定"),

    /** 账号已过期 */
    ACCOUNT_EXPIRED(403, "账号已过期"),

    /** 凭证已过期 */
    CREDENTIALS_EXPIRED(403, "登录凭证已过期"),

    /** 短信验证码错误或已过期 */
    SMS_CODE_INVALID(400, "验证码错误或已过期"),

    /** 发送过于频繁 */
    SMS_SEND_TOO_OFTEN(429, "发送过于频繁，请稍后再试"),

    /** 图形验证码错误 */
    CAPTCHA_INVALID(400, "图形验证码错误或已过期"),

    /** 同一账号在别处登录，旧 token 作废 */
    TOKEN_REVOKED(401, "账号已在其他设备登录，请重新登录"),

    /** 系统异常 */
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
