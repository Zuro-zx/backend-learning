package top.zsj.web.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 继承 RuntimeException（非受检异常），业务代码里直接 throw，
 * 由 GlobalExceptionHandler 统一转成 Result，不用到处 try-catch。
 * 只携带一个 ErrorCode，提示语默认取错误码的文案。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 出错时对应的错误码 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
