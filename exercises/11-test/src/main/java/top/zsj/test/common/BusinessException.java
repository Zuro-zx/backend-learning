package top.zsj.test.common;

/**
 * 业务异常：带错误码，由 {@link GlobalExceptionHandler} 统一翻译成 HTTP 响应。
 *
 * @author zsj
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
