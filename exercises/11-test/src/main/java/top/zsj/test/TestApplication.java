package top.zsj.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模块 11 启动类：一个刻意做得很小的下单试算服务，用来演示单元 / 切片 / 集成三层测试。
 *
 * @author zsj
 */
@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
