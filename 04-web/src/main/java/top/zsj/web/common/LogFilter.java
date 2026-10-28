package top.zsj.web.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日志过滤器（教程练习：自定义过滤器 + FilterRegistrationBean 注册）
 * <p>
 * 和 {@link RequestTimeFilter} 的区别：
 * RequestTimeFilter 标了 {@code @Component}，被自动注册到 /*（全路径）；
 * 本类故意不标 {@code @Component}，拦截路径（/api/log/hello）和执行顺序（order=2）
 * 统一交给 {@code FilterConfig} 里的 {@code FilterRegistrationBean} 控制。
 * <p>
 * 生命周期：init（容器启动时调用一次）→ doFilter（每次请求）→ destroy（容器关闭时调用一次）。
 *
 * @author zsj
 * @date 2026/10/28
 * @description 记录请求方法与请求参数的日志过滤器
 */
@Slf4j
public class LogFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("LogFilter 过滤器初始化");
        // FilterRegistrationBean 里 addInitParameter 塞进来的参数，这里能读到
        String exclusions = filterConfig.getInitParameter("exclusions");
        if (exclusions != null) {
            log.info("LogFilter 排除资源配置：{}", exclusions);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        // 获取客户端的请求方法
        String method = req.getMethod();
        // 获取请求参数（getParameterMap 对 query string 和表单都生效）
        String params = formatParams(req.getParameterMap());
        log.info("[LogFilter] 请求方法: {}，请求路径: {}，请求参数: {}", method, req.getRequestURI(), params);
        // 执行完过滤逻辑后，请求放行到下一个过滤器
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("LogFilter 过滤器销毁");
    }

    /** 把参数 Map 拼成可读的 k=v 串；Filter 是单例，别在这里存请求级状态 */
    private String formatParams(Map<String, String[]> parameterMap) {
        if (parameterMap.isEmpty()) {
            return "{}";
        }
        return parameterMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
