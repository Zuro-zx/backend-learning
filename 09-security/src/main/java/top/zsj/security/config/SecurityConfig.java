package top.zsj.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.zsj.security.security.DbUserDetailsService;
import top.zsj.security.security.JwtAuthenticationFilter;
import top.zsj.security.security.JwtProperties;
import top.zsj.security.security.RestAccessDeniedHandler;
import top.zsj.security.security.RestAuthenticationEntryPoint;

import java.util.List;

/**
 * 安全配置：过滤链、认证管理器、密码编码器、跨域
 * <p>
 * 四种鉴权粒度里的第一种写在这里：URL 规则（requestMatchers）。
 * /admin/** 整片路径的使用者都是管理员，一条规则挡住，不用逐个方法写注解。
 * 方法级鉴权由 @EnableMethodSecurity 打开，控制器上用 @PreAuthorize 声明权限点。
 * <p>
 * 会话策略 STATELESS：令牌无状态，不建 HttpSession，
 * 每个请求都靠 JwtAuthenticationFilter 重建身份。
 *
 * @author zsj
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final DbUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    /** BCrypt：自带盐，同一密码两次加密结果不同（BR-5） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 登录用的认证管理器：账号密码比对交给 DaoAuthenticationProvider */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // 账号停用要抛 DisabledException（转 30002），默认就是 true，这里写出来是为了讲清楚
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + 令牌认证，没有 Cookie 会话，CSRF 不适用
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 匿名可访问：连通性检查与登录换令牌
                        .requestMatchers("/public/**", "/auth/login").permitAll()
                        // 放行预检请求，浏览器不会给 OPTIONS 带令牌
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // URL 规则：账号管理整片给管理员，教师进来是 403
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 其余接口（/api/tasks/**、/auth/me）只要已认证，细粒度交给 @PreAuthorize
                        .anyRequest().authenticated())
                // 两个出口：401 没认出你是谁，403 认出了但不够格
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 令牌过滤器放在账号密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 跨域配置：本地联调走 Vite 代理用不到它，留给分离部署
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(jwtProperties.getCors().getAllowedOrigins().split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
