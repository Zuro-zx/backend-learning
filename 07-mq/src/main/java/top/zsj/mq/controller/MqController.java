package top.zsj.mq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description MqController
 */
@RestController
@RequestMapping("/mq")
public class MqController {

    @GetMapping("/hello")
    public String hello() {
        return "消息队列模块";
    }
}
