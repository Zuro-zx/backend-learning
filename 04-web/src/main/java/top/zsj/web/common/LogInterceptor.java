package top.zsj.web.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口耗时/访问日志拦截器
 * <p>
 * HandlerInterceptor 三个方法的执行顺序：
 * preHandle（Controller 前）→ Controller → postHandle（Controller 后，视图渲染前）
 * → afterCompletion（请求完全结束后，一定会执行）
 * <p>
 * preHandle 返回 false 表示中断请求，后面的 Controller 不会执行。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 记录接口耗时
 */
@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    /** 存开始时间的 key，放在 request 属性里，避免多线程互相干扰 */
    private static final String START_TIME = "logInterceptorStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());

        String desc = "非 Controller 方法";
        if (handler instanceof HandlerMethod hm) {
            desc = hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
        }
        log.info(">>> {} {}  处理器={}", request.getMethod(), request.getRequestURI(), desc);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME);
        long cost = start == null ? -1 : System.currentTimeMillis() - start;
        log.info("<<< {} {}  状态={}  耗时={}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                cost);
        if (ex != null) {
            log.error("请求处理异常：{}", ex.getMessage());
        }
    }
}
