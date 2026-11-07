package top.zsj.doc.common;

import lombok.Getter;

/**
 * 统一响应体
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

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<Void> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
