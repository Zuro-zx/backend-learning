package top.zsj.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 全局异常处理器
 * <p>
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody，
 * 会拦截所有 @RestController 抛出的异常，统一转成 JSON 响应体。
 * <p>
 * 好处：Controller 里只写正常逻辑，异常情况直接 throw，
 * 不用每个方法都 try-catch。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 一、自定义业务异常 —— 按业务码原样返回 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("业务异常 uri={} code={} msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.OK)          // 业务异常仍返回 200，靠 body 里的 code 区分
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    /** 二、@Valid / @Validated 校验 @RequestBody 失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = ErrorResponse.newErrors();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            // 同一字段多个错误时只保留第一条，避免被覆盖
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("参数校验失败：{}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "参数校验失败", errors));
    }

    /**
     * 三、校验方法参数失败（@RequestParam / @PathVariable 上的约束注解）
     * <p>
     * 这是 Spring 6.1+ 新增的异常类型，取代了以前的 ConstraintViolationException。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValid(HandlerMethodValidationException e) {
        Map<String, String> errors = ErrorResponse.newErrors();
        e.getAllValidationResults().forEach(result -> {
            String field = String.valueOf(result.getMethodParameter().getParameterName());
            result.getResolvableErrors().forEach(err -> errors.putIfAbsent(field, err.getDefaultMessage()));
        });
        log.warn("方法参数校验失败：{}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "参数校验失败", errors));
    }

    /** 四、缺少必传的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = String.format("缺少必传参数：%s（类型 %s）", e.getParameterName(), e.getParameterType());
        log.warn(msg);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, msg, Map.of(e.getParameterName(), "该参数为必传项")));
    }

    /** 五、参数类型对不上，比如 /user/abc 却要求 Long */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = String.format("参数 %s 的值「%s」类型不正确，应为 %s",
                e.getName(), e.getValue(),
                e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName());
        log.warn(msg);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, msg, Map.of(e.getName(), "类型不匹配")));
    }

    /** 六、请求体不是合法 JSON，或字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "请求体不是合法 JSON"));
    }

    /** 七、请求方式不被支持，比如用 GET 访问只支持 POST 的接口 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String msg = String.format("不支持 %s 请求，请使用 %s", e.getMethod(), e.getSupportedHttpMethods());
        log.warn(msg);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(405, msg));
    }

    /** 八、访问了不存在的静态资源或路径 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        log.warn("资源不存在：{}", e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "接口不存在：" + e.getResourcePath()));
    }

    /** 九、兜底：所有没预料到的异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 uri={}", request.getRequestURI(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "服务器内部错误：" + e.getMessage()));
    }
}
