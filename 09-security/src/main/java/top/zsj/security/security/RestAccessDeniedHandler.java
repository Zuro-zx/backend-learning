package top.zsj.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 403 出口：认出了你，但不够格
 * <p>
 * 触发场景：URL 规则拒绝（学生访问 /admin/**、学生调 GET /api/tasks）。
 * 返回真实 HTTP 403 + 统一响应体。
 * 前端对 403 只提示，不能把人踢下线——和 401 的区别就体现在这里（BR-7）。
 * <p>
 * 注意：@PreAuthorize 拒绝不经过这里（发生在 DispatcherServlet 之后），
 * 由 GlobalExceptionHandler 接住，两边都回 403 的同一结构。
 *
 * @author zsj
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":403,\"message\":\"无权访问该资源\",\"data\":null}");
    }
}
