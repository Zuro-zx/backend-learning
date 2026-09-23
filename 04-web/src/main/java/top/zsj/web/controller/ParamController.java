package top.zsj.web.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.web.common.R;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 参数绑定演示
 * <p>
 * 把 Spring MVC 常用的参数接收方式集中在一个类里，方便对照。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 各种参数绑定注解示例
 */
@Slf4j
@RestController
@RequestMapping("/param")
public class ParamController {

    /**
     * 必传参数：GET /param/required?name=zsj
     * <p>
     * 不传会抛 MissingServletRequestParameterException（400），
     * 被全局异常处理器接住。
     */
    @GetMapping("/required")
    public R<String> required(@RequestParam String name) {
        return R.ok("name = " + name);
    }

    /**
     * 可选参数 + 默认值：GET /param/default
     * <p>
     * required = false 表示可不传，defaultValue 表示不传时用默认值。
     * 注意：一旦写了 defaultValue，required 会自动变成 false。
     */
    @GetMapping("/default")
    public R<String> defaultValue(
            @RequestParam(required = false, defaultValue = "张三") String name,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return R.ok(String.format("name=%s, page=%d, size=%d", name, page, size));
    }

    /**
     * 多个同名参数 / 数组：GET /param/array?ids=1&ids=2&ids=3
     */
    @GetMapping("/array")
    public R<List<Long>> array(@RequestParam List<Long> ids) {
        return R.ok(ids);
    }

    /**
     * 逗号分隔自动转集合：GET /param/list?ids=1,2,3
     * <p>
     * Spring 默认就能把 "1,2,3" 拆成集合，不用自己 split。
     */
    @GetMapping("/list")
    public R<List<Long>> list(@RequestParam List<Long> ids) {
        return R.ok(ids);
    }

    /**
     * 一次性接收所有参数：GET /param/map?a=1&b=2
     * <p>
     * 适合参数不固定的场景（比如动态筛选条件）。
     */
    @GetMapping("/map")
    public R<Map<String, String>> map(@RequestParam Map<String, String> params) {
        return R.ok(params);
    }

    /**
     * 日期参数：GET /param/date?date=2026-09-23
     * <p>
     * 前端传的是字符串，加 @DateTimeFormat 告诉 Spring 按什么格式转 LocalDate。
     * 不加的话会用默认格式解析，格式不对就 400。
     */
    @GetMapping("/date")
    public R<LocalDate> date(@DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam LocalDate date) {
        return R.ok(date);
    }

    /**
     * 请求头：GET /param/header
     * <p>
     * curl -H "token: abc123" http://localhost:8004/param/header
     */
    @GetMapping("/header")
    public R<String> header(
            @RequestHeader("User-Agent") String userAgent,
            @RequestHeader(value = "token", required = false, defaultValue = "未携带") String token) {
        return R.ok(String.format("User-Agent=%s | token=%s", userAgent, token));
    }

    /**
     * 一次性拿所有请求头
     */
    @GetMapping("/headers")
    public R<Map<String, String>> headers(@RequestHeader Map<String, String> headers) {
        return R.ok(headers);
    }

    /**
     * 种一个 Cookie，方便下面测试：GET /param/set-cookie
     */
    @GetMapping("/set-cookie")
    public R<String> setCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("username", "zsj");
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        response.addCookie(cookie);
        return R.ok("Cookie 已写入：username=zsj，有效期 1 小时");
    }

    /**
     * 读取 Cookie：GET /param/cookie
     * <p>
     * 先访问 /param/set-cookie，再带上 Cookie 访问这个接口：
     * curl -b "username=zsj" http://localhost:8004/param/cookie
     */
    @GetMapping("/cookie")
    public R<String> cookie(@CookieValue(value = "username", required = false, defaultValue = "游客") String username) {
        return R.ok("当前用户：" + username);
    }

    /**
     * 原生 Servlet API：GET /param/servlet?name=zsj
     * <p>
     * Spring MVC 允许直接把 HttpServletRequest / HttpServletResponse
     * 写在方法参数上，需要拿原始对象时用。
     */
    @GetMapping("/servlet")
    public R<String> servlet(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String name = request.getParameter("name");
        String ip = request.getRemoteAddr();
        return R.ok(String.format("uri=%s, method=%s, name=%s, ip=%s", uri, method, name, ip));
    }

    /**
     * 表单提交（非 JSON）：POST /param/form  application/x-www-form-urlencoded
     * <p>
     * curl -X POST -d "name=zsj&age=21" http://localhost:8004/param/form
     */
    @PostMapping("/form")
    public R<String> form(@RequestParam String name, @RequestParam Integer age) {
        log.info("收到表单：name={}, age={}", name, age);
        return R.ok(String.format("表单接收成功：name=%s, age=%d", name, age));
    }
}
