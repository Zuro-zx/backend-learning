package top.zsj.security.common;

import lombok.Getter;

/**
 * 统一响应体：外层固定 code / message / data
 * <p>
 * 构造器 private，强制走 ok() / fail() 静态工厂，避免随手 new 出 code 与 message 对不上的响应体。
 *
 * @author zsj
 */
@Getter
public class Result<T> {

    private final Integer code;

    private final String message;

    private final T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}
