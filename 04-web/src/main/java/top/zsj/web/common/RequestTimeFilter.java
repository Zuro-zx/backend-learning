package top.zsj.web.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求总耗时过滤器
 * <p>
 * Filter 和 Interceptor 的区别（两个都实现了一遍，方便对比）：
 * <ul>
 *   <li>Filter 属于 Servlet 容器（Tomcat）规范，在 DispatcherServlet 之前 / 之后执行；</li>
 *   <li>Interceptor 属于 Spring MVC，在 DispatcherServlet 之内、Handler 之前 / 之后执行。</li>
 * </ul>
 * 所以同一个请求，Filter 统计到的耗时 ≥ Interceptor 统计到的耗时，
 * 差值是 Spring MVC 自身的分发开销。
 * <p>
 * 标了 @Component 之后 Spring Boot 会自动把它注册到 /*（所有路径），
 * 不需要再写 FilterRegistrationBean。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 统计请求总耗时
 */
@Slf4j
@Component
@Order(1)
public class RequestTimeFilter implements Filter {

    /** 开始时间放在 request 属性里：Filter 是单例，用成员变量会被并发请求互相覆盖 */
    private static final String START_TIME = "requestTimeFilterStartTime";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        long start = System.currentTimeMillis();
        req.setAttribute(START_TIME, start);
        try {
            // 放行，请求会继续走到 DispatcherServlet -> Interceptor -> Controller
            chain.doFilter(request, response);
        } finally {
            // 放在 finally 里，保证请求抛异常时也能记上耗时
            long cost = System.currentTimeMillis() - start;
            log.info("[Filter]  {} {}  总耗时={}ms", req.getMethod(), req.getRequestURI(), cost);
        }
    }
}
