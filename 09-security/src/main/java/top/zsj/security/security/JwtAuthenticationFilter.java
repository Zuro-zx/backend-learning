package top.zsj.security.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 位置在 DispatcherServlet 之前、UsernamePasswordAuthenticationFilter 之前：
 * 只做一件事——把 Authorization: Bearer &lt;token&gt; 换成身份写进 SecurityContext。
 * 解析失败不在这里拦：不设置身份，继续往下走，让后面的 401 出口统一响应。
 * 这样「没带令牌」和「令牌坏了」两种情况的响应完全一致，前端只需处理一种。
 *
 * @author zsj
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final DbUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        // 契约 C-2：不带 Bearer 前缀一律按未认证处理
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                JwtTokenProvider.ParsedToken parsed = tokenProvider.parse(token);
                // 每个请求重新查库：账号停用或删除后，旧令牌立即失效
                UserDetails userDetails = userDetailsService.loadUserByUsername(parsed.username());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 过期、签名对不上、格式坏：不拦，交给 401 出口
                log.debug("令牌校验失败，按未认证处理：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (UsernameNotFoundException e) {
                log.debug("令牌里的账号已不存在，按未认证处理：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
