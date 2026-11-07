package top.zsj.monitor.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常翻译。
 *
 * @author zsj
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(Result.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(Result.fail(ErrorCode.PARAM_INVALID,
                        detail.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(Result.fail(ErrorCode.PARAM_INVALID, "请求体不是合法的 JSON"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未预期的异常", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }

    private String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}
