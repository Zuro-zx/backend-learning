package top.zsj.schedule.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.schedule.service.DailyWeatherService;

/**
 * 定时任务演示接口
 *
 * @author zsj
 * @date 2026/9/23
 * @description ScheduleController
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Resource
    private DailyWeatherService dailyWeatherService;

    @GetMapping("/hello")
    public String hello() {
        return "定时任务模块";
    }

    /**
     * 手动触发一次天气早报（用于验证配置和收件效果，不必等到每天 9:22 的定时点）
     */
    @GetMapping("/weather/send")
    public String sendWeather() {
        dailyWeatherService.sendDailyWeather();
        return "已触发，请查看应用日志与收件箱";
    }
}
