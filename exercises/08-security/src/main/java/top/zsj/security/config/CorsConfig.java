package top.zsj.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * 跨域配置
 * <p>
 * 必须挂在 Security 上（由 SecurityFilterChain 的 {@code .cors()} 去容器里找这个 bean）。
 * 只写 {@code WebMvcConfigurer.addCorsMappings} 或 Controller 上的 {@code @CrossOrigin} 不够：
 * Security 的过滤器链在最外层，请求在 AuthorizationFilter 就被 401 拦下了，根本走不到 MVC，
 * 401 响应上也就没有 CORS 头，浏览器会把整个响应丢掉，前端只看到
 * {@code TypeError: Failed to fetch}——连"401"都看不到。
 *
 * @author zsj
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        // 用 allowedOriginPatterns 而不是 allowedOrigins：前者支持通配（http://localhost:*），
        // 且响应里回显具体来源；后者写 * 时不能和 allowCredentials(true) 同时用
        config.setAllowedOriginPatterns(properties.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type",
                "X-Requested-With", "X-XSRF-TOKEN"));
        // 不写 exposedHeaders，JS 读不到响应头里的 Authorization 与 Content-Disposition
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
