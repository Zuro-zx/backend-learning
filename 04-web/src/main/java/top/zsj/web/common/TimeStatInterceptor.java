package top.zsj.web.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 接口耗时统计拦截器（教程练习）
 * <p>
 * 执行顺序：preHandle（Controller 前）→ Controller → postHandle（Controller 后，视图渲染前）
 * → afterCompletion（请求完全结束后，一定会执行，异常也逃不掉）。
 * <p>
 * 和 {@link LogInterceptor} 的区别：LogInterceptor 记录访问日志 + 状态码 + 耗时，
 * 本类专注统计每个接口的执行耗时，日志里带上线程名方便和 Filter 的日志对照执行顺序。
 *
 * @author zsj
 * @date 2026/10/28
 * @description 统计每个接口的执行耗时
 */
@Slf4j
@Component
public class TimeStatInterceptor implements HandlerInterceptor {

    /** 存储请求开始时间的属性名 */
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);
        // 放行请求
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // 控制器执行后调用（此时还未返回视图）
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求处理结束耗时（计算耗时）
        long startTime = (Long) request.getAttribute(START_TIME);
        long endTime = System.currentTimeMillis();
        long cost = endTime - startTime;
        // 打印日志，handler.getClass() 获取处理器类名，带上线程名方便对照执行顺序
        log.info("[TimeStat] 接口耗时 - 方法: {}，耗时: {}ms，线程: {}",
                handler.getClass().getSimpleName(), cost, Thread.currentThread().getName());
    }
}
