package top.zsj.test.common;

/**
 * 统一响应体。
 *
 * @param code    业务码，0 表示成功
 * @param message 提示信息
 * @param data    业务数据
 * @param <T>     业务数据类型
 * @author zsj
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}
