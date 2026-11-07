package top.zsj.monitor.common;

/**
 * 统一响应体。
 *
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
