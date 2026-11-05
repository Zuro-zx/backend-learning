package top.zsj.security.common;

import lombok.Data;

/**
 * 统一响应体
 * <p>
 * 外层固定 code、message、data 三个字段（契约 C-1），data 在失败时为 null。
 * 构造器 private：强制走 ok() / fail() 静态工厂，避免随手 new 出 code 对不上的响应体。
 *
 * @author zsj
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，业务错误 30001 起，HTTP 对齐码见 ErrorCode */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据，失败时为 null */
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

    /** 失败，用预定义错误码的默认提示语 */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败，自定义提示语（提示语要区分场景时用，如 30001 与 30002 文案不同） */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
