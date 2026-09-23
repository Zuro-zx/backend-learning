package top.zsj.web.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 运行时异常，抛出后由 GlobalExceptionHandler 统一转成响应体，
 * 业务代码里不用到处 try-catch，也不用每个方法都返回 R.fail()。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务状态码 */
    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
