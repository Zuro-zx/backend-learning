package top.zsj.security.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import top.zsj.security.security.JsonAccessDeniedHandler;
import top.zsj.security.security.JsonAuthenticationEntryPoint;
import top.zsj.security.security.JwtAuthenticationFilter;
import top.zsj.security.security.JwtService;
import top.zsj.security.security.RequestTraceFilter;
import top.zsj.security.security.TokenRegistry;
import top.zsj.security.sms.SmsCodeAuthenticationProvider;
import top.zsj.security.sms.SmsCodeService;

/**
 * 六条独立过滤器链：一个进程里同时演示六个阶段
 * <p>
 * 规则从上往下匹配、命中即停：{@code @Order} 小的先匹配，最后一条没有 securityMatcher，
 * 兜住所有剩余路径，所以必须放最后。把它挪到前面，前面的链永远匹配不到。
 * <p>
 * 与前五条的差别在练习 1：教程新增 /stage5 链时把 Order 调到第 ⑤ 条之前，
 * 这里直接把兜底链挪到 @Order(6)。
 *
 * @author zsj
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();

    private final JsonAccessDeniedHandler accessDeniedHandler = new JsonAccessDeniedHandler();

    private final RequestTraceFilter requestTraceFilter = new RequestTraceFilter();

    /** ① 全拦截：任何请求都要登录，用 HTTP Basic 拿身份 */
    @Bean
    @Order(1)
    public SecurityFilterChain stage1FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/stage1/**")
                // 最宽的一条规则，必须放最后——authorizeHttpRequests 内部也是从上往下匹配
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /** ② 白名单：公开路径放行，同一个路径按方法区分 */
    @Bean
    @Order(2)
    public SecurityFilterChain stage2FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/stage2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage2/public/**").permitAll()
                        // 读公开、写受保护：把方法也纳入匹配条件
                        .requestMatchers(HttpMethod.GET, "/stage2/articles/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /** ③ 角色与权限点：hasRole / hasAnyRole / hasAllRoles / hasAuthority */
    @Bean
    @Order(3)
    public SecurityFilterChain stage3FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/stage3/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage3/admin/**").hasRole("ADMIN")
                        .requestMatchers("/stage3/ops/**").hasAnyRole("ADMIN", "AUDITOR")
                        // hasAllRoles 是 Security 7 新增的，之前要写 hasRole('ADMIN') and hasRole('AUDITOR')
                        .requestMatchers("/stage3/audit/**").hasAllRoles("ADMIN", "AUDITOR")
                        .requestMatchers(HttpMethod.DELETE, "/stage3/articles/**")
                        .hasAuthority("ARTICLE_DELETE")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * ④ 表单登录：传统 Web 那一套，有登录页、有 Session、有 CSRF token
     * <p>
     * 这条链的错误响应保持默认（302 跳登录页），因为它演示的就是服务端渲染场景。
     */
    @Bean
    @Order(4)
    public SecurityFilterChain stage4FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager) throws Exception {
        http
                .securityMatcher("/stage4/**")
                .authorizeHttpRequests(auth -> auth
                        // 登录页、登录提交地址、取 CSRF 的辅助接口都要放行，
                        // 不放行会被"必须先登录"拦住，形成死循环
                        .requestMatchers("/stage4/login.html", "/stage4/login",
                                "/stage4/csrf", "/stage4/logout").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        // loginPage 与 loginProcessingUrl 不能同址，同址会渲染登录页死循环
                        .loginPage("/stage4/login.html")
                        .loginProcessingUrl("/stage4/login")
                        // 第二个参数 false 表示"总是跳这里"；true 表示跳回登录前想访问的页面
                        .defaultSuccessUrl("/stage4/me", false)
                        .failureUrl("/stage4/login.html?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/stage4/logout")
                        .logoutSuccessUrl("/stage4/login.html?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf
                        // Cookie 里的 token 允许 JS 读，静态页才能把它填进表单
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 默认的 XorCsrfTokenRequestAttributeHandler 会做 XOR 掩码，
                        // 表单里的值和 Cookie 里的值不是同一个字符串，手工比对永远对不上
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * ⑤ /stage5：练习 1 新增的 JWT 链
     * <p>
     * 顺序必须在兜底链之前，否则永远匹配不到。
     */
    @Bean
    @Order(5)
    public SecurityFilterChain stage5FilterChain(HttpSecurity http,
                                                 AuthenticationManager authenticationManager,
                                                 JwtService jwtService,
                                                 TokenRegistry tokenRegistry) throws Exception {
        http
                .securityMatcher("/stage5/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/stage5/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, tokenRegistry),
                        UsernamePasswordAuthenticationFilter.class)
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * ⑥ 前后端分离：JWT 无状态 API，兜底链
     * <p>
     * 没有 securityMatcher，兜住所有剩余路径，所以放最后。
     */
    @Bean
    @Order(6)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              AuthenticationManager authenticationManager,
                                              JwtService jwtService,
                                              TokenRegistry tokenRegistry) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 跨域预检不带 Authorization，拦住它等于所有跨域请求都失败
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // /error 必须放行：参数校验失败会 forward 到 /error，
                        // 它要登录的话 400 会被覆盖成 401
                        .requestMatchers("/", "/index.html", "/error",
                                "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/sms/**",
                                "/api/auth/captcha", "/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/**")
                        .hasAuthority("ARTICLE_DELETE")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 这三个默认开着，会在 AuthorizationFilter 之前直接返回 302 或弹登录框
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .cors(Customizer.withDefaults())
                // JWT 过滤器放在这里，保证授权判断时上下文里已经有身份
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, tokenRegistry),
                        UsernamePasswordAuthenticationFilter.class)
                // 追踪过滤器放在 AuthorizationFilter 之前，才能读到真实用户名
                .addFilterBefore(requestTraceFilter, AuthorizationFilter.class)
                .authenticationManager(authenticationManager);
        return http.build();
    }

    /**
     * 认证管理器：密码 + 短信两种凭证，各一个 Provider
     * <p>
     * ProviderManager 按 supports() 挑一个，顺序无所谓。容器里有两个 UserDetailsService，
     * 按类型注入会报 NoUniqueBeanDefinitionException，所以用 @Qualifier 指定 Bean 名。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            @Qualifier("dbUserDetailsService") UserDetailsService dbUserDetailsService,
            @Qualifier("phoneUserDetailsService") UserDetailsService phoneUserDetailsService,
            PasswordEncoder passwordEncoder,
            SmsCodeService smsCodeService) {

        // Security 7 把 setUserDetailsService 改成了构造器参数
        DaoAuthenticationProvider passwordProvider = new DaoAuthenticationProvider(dbUserDetailsService);
        passwordProvider.setPasswordEncoder(passwordEncoder);

        SmsCodeAuthenticationProvider smsProvider =
                new SmsCodeAuthenticationProvider(smsCodeService, phoneUserDetailsService);

        return new ProviderManager(passwordProvider, smsProvider);
    }

    /** 委托式密码编码器：按密文前缀选算法，同一个库里可以混存 {bcrypt} / {noop} */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
