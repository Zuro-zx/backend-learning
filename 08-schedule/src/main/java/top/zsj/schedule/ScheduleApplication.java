package top.zsj.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类：通过 @EnableScheduling 开启定时任务，
 * 具体任务在 TaskService 中用 @Scheduled 的 cron 表达式定义。
 *
 * @author zsj
 * @date 2026/9/23
 * @description ScheduleApplication
 */
@EnableScheduling
@SpringBootApplication
public class ScheduleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }
}
