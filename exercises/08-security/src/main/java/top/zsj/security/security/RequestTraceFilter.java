package top.zsj.security.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求追踪：记录每个请求最终被哪个身份访问
 * <p>
 * 必须挂在 {@code AuthorizationFilter} 之前、认证过滤器之后，才能读到真实用户名；
 * 挂得比认证过滤器还早，读到的永远是 anonymous。
 * <p>
 * 状态码有个坑：授权拒绝抛出的 AccessDeniedException 要一路抛到外层
 * ExceptionTranslationFilter 才被转成 403，等本过滤器的 finally 执行时响应体还没写，
 * {@code response.getStatus()} 仍然是 200。所以异常分支要自己按异常类型折算状态码，
 * 否则日志里会出现"被拒的请求记成 200"。
 *
 * @author zsj
 */
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 认证过滤器已经跑过，这里读到的就是真实身份
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = isAnonymous(authentication);
        String user = anonymous ? "anonymous" : authentication.getName();
        long start = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
            logLine(request, user, response.getStatus(), start);
        } catch (AuthenticationException e) {
            logLine(request, user, HttpServletResponse.SC_UNAUTHORIZED, start);
            throw e;
        } catch (AccessDeniedException e) {
            // 匿名身份访问受保护资源时抛的也是 AccessDeniedException，
            // 真正写回的是 401（ExceptionTranslationFilter 发现身份是 anonymous 就转 entry point）
            logLine(request, user, anonymous
                    ? HttpServletResponse.SC_UNAUTHORIZED
                    : HttpServletResponse.SC_FORBIDDEN, start);
            throw e;
        }
    }

    private boolean isAnonymous(Authentication authentication) {
        // 匿名身份也是"已认证"的（AnonymousAuthenticationToken），要单独判断
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }

    private void logLine(HttpServletRequest request, String user, int status, long start) {
        log.info("[trace] {} {} -> user={}, status={}, {}ms",
                request.getMethod(), request.getRequestURI(), user, status,
                System.currentTimeMillis() - start);
    }
}
