package top.zsj.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.web.common.Result;

/**
 * 过滤器验证接口
 * <p>
 * /api/log/hello 被 {@code FilterConfig} 里注册的 LogFilter 拦截，
 * 访问时带上参数即可在控制台看到 LogFilter 打印的请求方法与参数。
 *
 * @author zsj
 * @date 2026/10/28
 * @description 用于验证 LogFilter 生效的带参接口
 */
@RestController
@RequestMapping("/api/log")
public class LogController {

    /**
     * GET /api/log/hello?name=zsj&age=21
     */
    @GetMapping("/hello")
    public Result<String> hello(
            @RequestParam(required = false, defaultValue = "world") String name,
            @RequestParam(required = false, defaultValue = "0") Integer age) {
        return Result.ok(String.format("hello, %s（age=%d），请求已穿过 LogFilter", name, age));
    }
}
