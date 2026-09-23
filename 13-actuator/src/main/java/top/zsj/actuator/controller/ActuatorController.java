package top.zsj.actuator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description ActuatorController
 */
@RestController
@RequestMapping("/actuator")
public class ActuatorController {

    @GetMapping("/hello")
    public String hello() {
        return "监控与 Actuator 模块";
    }
}
