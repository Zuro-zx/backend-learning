package top.zsj.redis.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.zsj.redis.common.BusinessException;
import top.zsj.redis.common.ErrorCode;
import top.zsj.redis.common.JwtUtils;

/**
 * 登录态校验拦截器
 * <p>
 * 读取 Authorization 头里的 Bearer token，验签通过后把载荷放进请求属性，
 * Controller 用 @RequestAttribute 直接取，不用自己解析请求头。
 * <p>
 * 跨域预检（OPTIONS）必须放行：浏览器不会给预检请求带 Authorization，
 * 这里拦下来会导致预检拿不到跨域响应头，真正的请求也会被浏览器一起拦掉。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 登录态拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性名，Controller 用 @RequestAttribute 取值 */
    public static final String ATTR_CLAIMS = "claims";

    private static final String HEADER_NAME = "Authorization";

    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER_NAME);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Claims claims = jwtUtils.parse(header.substring(TOKEN_PREFIX.length()));
        request.setAttribute(ATTR_CLAIMS, claims);
        return true;
    }
}
