package top.zsj.logging.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志管理接口
 *
 * <p>演示 SLF4J 日志门面在 Spring Boot 中的使用：Lombok {@code @Slf4j}
 * 自动注入 {@code log} 对象，日志级别由低到高为
 * trace &lt; debug &lt; info &lt; warn &lt; error，默认只输出 info 及以上级别。</p>
 *
 * @author zsj
 * @date 2026/9/23
 * @description 日志管理接口
 */
@RestController
@RequestMapping("/logging")
@Slf4j
public class LoggingController {

    @GetMapping("/hello")
    public String hello() {
        return "日志管理 模块";
    }

    @GetMapping("/demo")
    public String demo() {
        log.info("处理 /logging/demo 请求");
        log.warn("这是一条警告日志");
        log.error("这是一条错误日志");
        return "ok";
    }
}
