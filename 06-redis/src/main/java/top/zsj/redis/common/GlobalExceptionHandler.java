package top.zsj.redis.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局统一异常处理（从 04-web 复制）
 * <p>
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody，
 * 会拦截所有 @RestController 抛出的异常，统一转成 Result 响应体。
 * 好处：Controller 里只写正常逻辑，异常情况直接 throw，不用到处 try-catch。
 * <p>
 * 返回值直接是 Result（HTTP 状态码一律 200），真正的业务状态看 body 里的 code ——
 * 这样前端拦截器不会把「资源不存在」当成网络故障去弹错误页。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 全局统一异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 一、自定义业务异常：Service 里 throw 的都走这里 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    /** 二、@Valid 校验 @RequestBody 失败：取第一条字段错误的提示语 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    /** 三、参数类型对不上，比如 /users/abc 却要求 Long */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数类型错误");
    }

    /** 四、缺少必填的请求参数，比如 /sms/send 没带 phone */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /** 五、请求方法不支持，比如 GET /sms/login（只接受 POST） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMethod());
        return Result.fail(405, "请求方法不支持：" + e.getMethod());
    }

    /** 六、请求体类型不支持，比如用了 text/plain 而非 application/json */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaType(HttpMediaTypeNotSupportedException e) {
        log.warn("请求体类型不支持：{}", e.getContentType());
        return Result.fail(415, "请求体类型不支持，请使用 application/json");
    }

    /** 七、请求体不是合法 JSON，或字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /** 八、访问了不存在的路径 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    /** 九、兜底：所有没预料到的异常（绝不能把堆栈直接丢给前端，只回一句通用提示） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
