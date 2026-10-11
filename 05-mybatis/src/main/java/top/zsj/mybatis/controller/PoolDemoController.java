package top.zsj.mybatis.controller;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.mybatis.mapper.UserMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连接池实验接口（实验专用）：
 * 1. /pool/sleep 让当前请求占用一个连接 N 秒，配合并发请求即可把池打满，
 *    复现 maximum-pool-size = 1 时的 Connection is not available
 * 2. /pool/stats 输出 HikariCP 活跃/空闲/等待连接数，等价于课程里 Druid 监控页的角色
 *
 * @author zsj
 */
@RestController
@RequestMapping("/pool")
@RequiredArgsConstructor
public class PoolDemoController {

    private final UserMapper userMapper;
    private final HikariDataSource dataSource;

    @GetMapping("/sleep")
    public String sleep(@RequestParam(defaultValue = "2") int seconds) {
        long start = System.currentTimeMillis();
        userMapper.sleep(Math.max(1, Math.min(seconds, 10)));
        return "占用连接 " + (System.currentTimeMillis() - start) + " ms";
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("poolName", dataSource.getPoolName());
        stats.put("maximumPoolSize", dataSource.getMaximumPoolSize());
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool != null) {
            stats.put("activeConnections", pool.getActiveConnections());
            stats.put("idleConnections", pool.getIdleConnections());
            stats.put("totalConnections", pool.getTotalConnections());
            stats.put("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
        }
        return stats;
    }
}
