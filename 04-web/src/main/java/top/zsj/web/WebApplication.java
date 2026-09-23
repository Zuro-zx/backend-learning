package top.zsj.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 04-web 启动类
 *
 * @author zsj
 * @date 2026/9/23
 * @description Web 开发模块启动入口，端口 8004
 */
@SpringBootApplication
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
