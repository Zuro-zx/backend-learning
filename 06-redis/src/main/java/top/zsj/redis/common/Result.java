package top.zsj.redis.common;

import lombok.Data;

/**
 * 统一响应体（从 04-web 复制）
 * <p>
 * 前后端分离项目里 Controller 不直接返回业务对象，而是统一包一层
 * {code, message, data}，前端只判断 code 即可，不用去猜 HTTP 状态码的含义。
 * <p>
 * 构造器是 private 的：强制走 ok() / fail() 静态工厂，
 * 避免有人随手 new 出一个 code 对不上的响应体。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 统一响应体
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，其余为失败，具体见 ErrorCode */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功，不带数据 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 成功，带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /** 失败，用预定义的错误码 */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败，自定义状态码与提示语 */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
