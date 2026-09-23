package top.zsj.schedule.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description ScheduleController
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @GetMapping("/hello")
    public String hello() {
        return "定时任务模块";
    }
}
