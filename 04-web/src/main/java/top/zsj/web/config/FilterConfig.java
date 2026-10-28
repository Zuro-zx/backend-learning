package top.zsj.web.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.zsj.web.common.LogFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器注册配置
 * <p>
 * {@code FilterRegistrationBean} 比 {@code @Component} 注册过滤器多了三项能力：
 * 指定拦截路径（urlPatterns）、指定执行顺序（order，数值越小越先执行）、
 * 传初始化参数（initParameter）。
 * <p>
 * 请求进来时过滤器的实际顺序：
 * RequestTimeFilter（order=1，全路径）→ LogFilter（order=2，仅 /api/log/hello）
 * → DispatcherServlet → Interceptor → Controller。
 *
 * @author zsj
 * @date 2026/10/28
 * @description 注册 LogFilter 并指定拦截路径与执行顺序
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<LogFilter> logFilterFilterRegistrationBean() {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        // 设置过滤器实例
        registration.setFilter(new LogFilter());
        // 设置拦截路径（/* 为匹配所有请求；这里只让它对日志接口生效）
        List<String> urlPatterns = new ArrayList<>();
        urlPatterns.add("/api/log/hello");
        registration.setUrlPatterns(urlPatterns);
        // 设置执行顺序（数值越小越先执行）
        registration.setOrder(2);
        // 排除静态资源（可选）：以初始化参数的形式传给过滤器
        registration.addInitParameter("exclusions", "*.js,*.css,*.png");
        return registration;
    }
}
