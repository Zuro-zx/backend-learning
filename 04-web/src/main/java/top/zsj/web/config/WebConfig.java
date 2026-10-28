package top.zsj.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.zsj.web.common.LogInterceptor;
import top.zsj.web.common.TimeStatInterceptor;

/**
 * Web 层配置
 * <p>
 * 实现 WebMvcConfigurer 可以在不改动 Spring 默认配置的前提下，
 * 追加拦截器、跨域、静态资源、消息转换器等。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 注册拦截器与跨域配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 构造器注入时间统计拦截器（TimeStatInterceptor 标了 @Component） */
    private final TimeStatInterceptor timeStatInterceptor;

    /** 注册拦截器，并指定拦截/放行的路径 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册时间统计拦截器
        registry.addInterceptor(timeStatInterceptor)
                // 拦截所有路径
                .addPathPatterns("/**")
                // 排除静态资源和错误页
                .excludePathPatterns("/static/**", "/error")
                // 执行顺序（数值越小越先执行）
                .order(1);

        registry.addInterceptor(new LogInterceptor())
                // 拦截所有接口
                .addPathPatterns("/**")
                // 排除掉不需要记日志的路径
                .excludePathPatterns("/error")
                // 晚于 TimeStatInterceptor 执行
                .order(2);
    }

    /**
     * 跨域配置（前后端分离必备）
     * <p>
     * allowedOriginPatterns("*") 比 allowedOrigins("*") 更宽松，
     * 允许携带 Cookie 的场景下也能用。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
