package top.zsj.doc.common;

import lombok.Getter;

/**
 * 业务异常：携带错误码，由 GlobalExceptionHandler 统一转成响应体
 *
 * @author zsj
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
