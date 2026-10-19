package top.zsj.zhihu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 知乎专题接口服务启动类
 *
 * @author zsj
 * @date 2026/10/19
 * @description 知乎专题接口服务启动类
 */
@SpringBootApplication
@MapperScan("top.zsj.zhihu.mapper")
public class ZhihuApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhihuApplication.class, args);
    }
}
