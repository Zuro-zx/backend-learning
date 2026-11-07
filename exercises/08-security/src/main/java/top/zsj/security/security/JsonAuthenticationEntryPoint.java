package top.zsj.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import top.zsj.security.common.ErrorCode;

import java.io.IOException;

/**
 * 统一 401：未登录或登录已过期
 * <p>
 * 默认实现会按 {@code Accept} 协商：浏览器请求 302 跳登录页，API 请求 401 加
 * {@code WWW-Authenticate} 头。前后端分离下两种都不能用——302 前端拿到的是 HTML 登录页，
 * {@code WWW-Authenticate} 会让浏览器弹出原生账号密码框。
 * <p>
 * 所以这里不写 {@code WWW-Authenticate}，只回 JSON。
 *
 * @author zsj
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** 过滤器可以把"为什么失效"写进这个 request 属性，让这里给出更准的提示 */
    public static final String FAILURE_MESSAGE_ATTRIBUTE = "auth.failureMessage";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attribute = request.getAttribute(FAILURE_MESSAGE_ATTRIBUTE);
        String message = attribute instanceof String text ? text : ErrorCode.UNAUTHORIZED.getMessage();
        JsonResponses.write(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), message);
    }
}
