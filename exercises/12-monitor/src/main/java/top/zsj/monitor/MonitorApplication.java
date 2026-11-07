package top.zsj.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模块 12 启动类：一个只做「下单 + 库存」的最小业务，配套 Actuator 与 Micrometer。
 *
 * @author zsj
 */
@SpringBootApplication
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
