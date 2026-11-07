package top.zsj.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析凭证，把身份写进 SecurityContext
 * <p>
 * 三个设计决定：
 * <ol>
 *     <li>验签失败不直接返回 401，而是清空上下文继续放行，由后面的 AuthorizationFilter 统一拒绝，
 *     这样"未登录"的错误响应只有一份实现；</li>
 *     <li>新建 context 再整体塞进去，避免 ThreadLocal 串线程；</li>
 *     <li>练习 2 / 3：解析成功后还要查一次白名单，jti 已被顶掉就当作未登录。</li>
 * </ol>
 *
 * @author zsj
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final TokenRegistry tokenRegistry;

    public JwtAuthenticationFilter(JwtService jwtService, TokenRegistry tokenRegistry) {
        this.jwtService = jwtService;
        this.tokenRegistry = tokenRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(JwtService.BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(header.substring(JwtService.BEARER_PREFIX.length()));
            String username = claims.getSubject();
            String jti = claims.getId();

            // 练习 2 / 3：主动失效与单端登录
            if (!tokenRegistry.isActive(username, jti)) {
                log.debug("token 已失效：username={}, jti={}", username, jti);
                request.setAttribute(JsonAuthenticationEntryPoint.FAILURE_MESSAGE_ATTRIBUTE,
                        "账号已在其他设备登录，请重新登录");
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            List<?> raw = claims.get(JwtService.CLAIM_AUTHORITIES, List.class);
            List<GrantedAuthority> authorities = raw == null ? List.of()
                    : raw.stream().map(String::valueOf)
                    .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                    .toList();

            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    username, null, authorities);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException e) {
            // 过期、签名不符、结构不对：一律当未登录处理，不在这里写响应
            log.debug("token 解析失败：{}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
