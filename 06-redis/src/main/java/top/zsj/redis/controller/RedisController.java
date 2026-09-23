package top.zsj.redis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description RedisController
 */
@RestController
@RequestMapping("/redis")
public class RedisController {

    @GetMapping("/hello")
    public String hello() {
        return "缓存与 Redis 模块";
    }
}
