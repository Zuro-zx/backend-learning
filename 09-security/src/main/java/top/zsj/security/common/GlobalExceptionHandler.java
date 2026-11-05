package top.zsj.security.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局统一异常处理
 * <p>
 * 三个出口要分清（需求文档第 08 节「异常出口」）：
 * <ul>
 *   <li>未带令牌、令牌坏   → RestAuthenticationEntryPoint   401（过滤器阶段，进不到这里）</li>
 *   <li>URL 规则拒绝       → RestAccessDeniedHandler        403（同上）</li>
 *   <li>@PreAuthorize 拒绝 → 这里（AccessDeniedException）   403</li>
 *   <li>归属不符           → BusinessException(FORBIDDEN)   403</li>
 * </ul>
 * code 为 401/403 时返回真实 HTTP 状态码（前端靠它决定跳登录还是只提示），
 * 其余业务错误一律 HTTP 200，靠 body 里的 code 区分。
 *
 * @author zsj
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 一、自定义业务异常：Service 里 throw 的都走这里 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常：code={}，{}", e.getErrorCode().getCode(), e.getMessage());
        return wrap(e.getErrorCode());
    }

    /** 二、@PreAuthorize 方法级鉴权拒绝：注意这里必须回真实 403，不是 200 里包 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("方法级鉴权拒绝：{}", e.getMessage());
        return wrap(ErrorCode.FORBIDDEN);
    }

    /** 三、@Valid 校验 @RequestBody 失败：取第一条字段错误的提示语 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("参数校验失败");
        return ResponseEntity.ok(Result.fail(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    /** 四、参数类型对不上，比如 /api/tasks/abc 却要求 Long */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数类型错误"));
    }

    /** 五、请求体不是合法 JSON，或字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误"));
    }

    /** 六、访问了不存在的路径，落到静态资源兜底 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        return wrap(ErrorCode.NOT_FOUND);
    }

    /** 七、兜底：所有没预料到的异常（日志里留堆栈，响应里不出现） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("系统异常", e);
        return wrap(ErrorCode.INTERNAL_ERROR);
    }

    /** 401/403 改写 HTTP 状态码，其余一律 HTTP 200 */
    private ResponseEntity<Result<Void>> wrap(ErrorCode errorCode) {
        Result<Void> body = Result.fail(errorCode);
        return errorCode.isHttpStatus()
                ? ResponseEntity.status(errorCode.getCode()).body(body)
                : ResponseEntity.ok(body);
    }
}
