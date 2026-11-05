package top.zsj.security.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 继承 RuntimeException，业务代码里直接 throw，由 GlobalExceptionHandler 统一转响应。
 * 大多数业务错误 HTTP 状态仍是 200；code 为 401/403 时由全局异常处理改写 HTTP 状态码。
 * 支持覆盖默认提示语：比如登录失败 30001 和账号停用 30002 的文案必须区分开。
 *
 * @author zsj
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 出错时对应的错误码 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 用自定义提示语覆盖错误码默认文案 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
