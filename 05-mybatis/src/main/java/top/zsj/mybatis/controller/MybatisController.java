package top.zsj.mybatis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description MybatisController
 */
@RestController
@RequestMapping("/mybatis")
public class MybatisController {

    @GetMapping("/hello")
    public String hello() {
        return "持久层与 MyBatis 模块";
    }
}
