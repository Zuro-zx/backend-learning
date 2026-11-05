package top.zsj.security.common;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 4xx 与 5xx 跟 HTTP 状态码对齐，3xxxx 是业务错误（错误码表见需求文档第 04 节）。
 * 业务错误大多数仍是 HTTP 200，靠 code 区分；
 * 只有 401 与 403 会改 HTTP 状态码，因为前端要靠它决定行为（契约 C-3）：
 * 401 清登录态跳登录页，403 只提示不踢人。
 *
 * @author zsj
 */
@Getter
public enum ErrorCode {

    /** 成功 */
    SUCCESS(200, "success"),

    // ---- 与 HTTP 状态码对齐 ----
    /** 参数错误：校验失败、JSON 解析失败、路径参数类型不对 */
    BAD_REQUEST(400, "参数错误"),
    /** 未认证或令牌已失效：没带令牌、令牌过期、签名对不上 */
    UNAUTHORIZED(401, "未认证或令牌已失效"),
    /** 无权访问该资源：角色不符、权限点不够、看了别人的任务 */
    FORBIDDEN(403, "无权访问该资源"),
    /** 资源不存在：路径写错 */
    NOT_FOUND(404, "资源不存在"),
    /** 系统繁忙：未预料的异常，日志里留堆栈，响应里不出现 */
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    // ---- 业务错误码 ----
    /** 登录时账号不存在或密码不对，两种情况提示语一样，不告诉对方账号是否存在 */
    BAD_CREDENTIALS(30001, "用户名或密码错误"),
    /** 密码对了但 enabled 为假（演示账号：guest） */
    ACCOUNT_DISABLED(30002, "账号已被禁用"),
    /** 删除一个查不到的账号 */
    USER_NOT_FOUND(30003, "用户不存在"),
    /** 新增账号撞唯一键之前先查一次 */
    USERNAME_EXISTS(30004, "用户名已存在"),
    /** 管理员删到自己，防止把唯一的管理员删没 */
    CANNOT_DELETE_SELF(30005, "不能删除当前登录账号"),
    /** 批改或查看一个不存在的任务 id */
    TASK_NOT_FOUND(30010, "任务不存在"),
    /** 同一学生同标题任务仍是待批改时重复提交 */
    TASK_ALREADY_SUBMITTED(30011, "同一任务已提交，等待批改"),
    /** 教师对同一条任务打第二次分 */
    TASK_ALREADY_REVIEWED(30012, "该任务已批改，不能重复批改");

    /** 业务状态码 */
    private final Integer code;

    /** 默认提示语 */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 401 与 403 要改 HTTP 状态码，其余业务错误一律 HTTP 200 */
    public boolean isHttpStatus() {
        return code == 401 || code == 403;
    }
}
