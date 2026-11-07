package top.zsj.security.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 * <p>
 * 这里是「错误响应的第三处实现」：{@code @PreAuthorize} 拒绝时抛的是
 * AuthorizationDeniedException，它在 Controller 调用栈里抛出，走不到
 * ExceptionTranslationFilter，所以过滤器链上的 JsonAccessDeniedHandler 接不到它，
 * 必须在这里再兜一次，否则会被兜底成 500。
 *
 * @author zsj
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("业务异常：code={}, message={}", code.getCode(), e.getMessage());
        return ResponseEntity.status(code.getCode()).body(Result.fail(code, e.getMessage()));
    }

    /** 方法级鉴权拒绝（@PreAuthorize）：403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("方法级鉴权拒绝：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(ErrorCode.FORBIDDEN));
    }

    /** 认证失败：由登录接口显式抛出的分支走到这里，401 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(ErrorCode.BAD_CREDENTIALS));
    }

    /** 其他认证异常 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(ErrorCode.UNAUTHORIZED));
    }

    /** 参数校验失败（@Valid @RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(ErrorCode.BAD_REQUEST, detail));
    }

    /** 缺请求参数（短信发码漏了 phone 之类） */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.BAD_REQUEST, "缺少参数：" + e.getParameterName()));
    }

    /** 兜底：日志留堆栈，响应里不出现内部细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未预料的异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }
}
