package top.zsj.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import top.zsj.security.common.ErrorCode;

import java.io.IOException;

/**
 * 统一 403：认出来了，但没有权限
 * <p>
 * 只接得住 {@code AuthorizationFilter} 拒绝的那一类。{@code @PreAuthorize} 抛的
 * AuthorizationDeniedException 在 Controller 调用栈里，走不到 ExceptionTranslationFilter，
 * 由 GlobalExceptionHandler 兜（三处实现中的第三处）。
 *
 * @author zsj
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        JsonResponses.write(response, HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }
}
