package top.zsj.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description SecurityController
 */
@RestController
@RequestMapping("/security")
public class SecurityController {

    @GetMapping("/hello")
    public String hello() {
        return "安全认证模块";
    }
}
