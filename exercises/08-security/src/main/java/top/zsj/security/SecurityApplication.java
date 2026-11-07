package top.zsj.security;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 08-security 启动类
 * <p>
 * 教程原文包名是 top.mqxu.security，这里按本仓库约定统一改成 top.zsj.security。
 *
 * @author zsj
 */
@SpringBootApplication
@MapperScan("top.zsj.security.mapper")
@ConfigurationPropertiesScan
public class SecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}
