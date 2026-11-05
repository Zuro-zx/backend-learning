package top.zsj.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 401 出口：没认出你是谁
 * <p>
 * 触发场景：没带令牌、令牌过期、签名对不上、令牌里的账号已删除。
 * 返回真实 HTTP 401 + 统一响应体 {code:401,message:...,data:null}，
 * 前端拦截器据此清登录态跳登录页（契约 C-3）。
 * <p>
 * 手写 JSON 而不是注入 ObjectMapper：这里只有固定三个字段，
 * 不引入 Jackson 2/3 的类型差异，过滤器阶段也不该依赖 Web 层组件。
 *
 * @author zsj
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":401,\"message\":\"未认证或令牌已失效\",\"data\":null}");
    }
}
