package top.zsj.schedule.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 定时任务服务类：用两种 cron 表达式实现两种定时任务效果
 * <p>
 * Cron 表达式共 6 位：秒 分 时 日 月 周（Spring 不支持第 7 位的年份）
 * <ul>
 *   <li>「指定时间触发」：固定时间执行，如 cron="0 0 9 * * ?" 表示每天 9 点 0 分 0 秒；</li>
 *   <li>「每隔 N 秒触发」：固定频率执行，如 cron="0/3 * * * * ?" 表示每分钟的第 0、3、6……秒各执行一次，即每隔 3 秒一次。</li>
 * </ul>
 *
 * @author zsj
 * @date 2026/10/21
 * @description TaskService
 */
@Service
@Slf4j
public class TaskService {

    /**
     * 任务 1：在指定时间触发 —— 每天 9 点 0 分 0 秒执行一次
     * <p>
     * cron 六位依次是：秒(0) 分(0) 时(9) 日(*) 月(*) 周(?)。
     * 日和周只能有一个指定值，另一个必须用 ? 占位。
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void fixedTimeTask() {
        log.info("【指定时间任务】每天 9 点触发，执行时间: {}，执行线程: {}",
                LocalDateTime.now(), Thread.currentThread().getName());
    }

    /**
     * 任务 2：每隔 3 秒触发 —— 0/3 表示从第 0 秒开始每 3 秒一次
     * <p>
     * 也可用 fixedRate = 3000 达到同样效果，但作业要求用 cron 表达式实现，故保持 cron 写法。
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void intervalTask() {
        log.info("【每隔3秒任务】执行时间: {}，执行线程: {}",
                LocalDateTime.now(), Thread.currentThread().getName());
    }
}
