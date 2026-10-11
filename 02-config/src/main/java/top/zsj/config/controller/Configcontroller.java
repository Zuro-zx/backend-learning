package top.zsj.config.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.config.properties.AppProperties;
import top.zsj.config.service.EnvService;

/**
 * 配置查询接口
 *
 * <p>演示 @Value 直接注入，以及 @Profile 环境切换、
 * {@code @ConfigurationProperties} + {@code @Validated} 的绑定结果。</p>
 *
 * @author zsj
 * @date 2026/9/9
 * @description 配置查询接口
 */
@RestController
@RequestMapping("/config")
public class Configcontroller {

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${zsj.name}")
    private String myname;

    @Value("${zsj.job}")
    private String myjob;

    private final EnvService envService;

    private final AppProperties appProperties;

    public Configcontroller(EnvService envService, AppProperties appProperties) {
        this.envService = envService;
        this.appProperties = appProperties;
    }

    @GetMapping("/basic")
    public String getBasicInfo() {
        return "服务器端口是：" + this.serverPort + ",应用名称是：" + appName;
    }

    @GetMapping("/my")
    public String getMyInfo() {
        return "我的姓名是：" + this.myname + ",我的职业是：" + myjob;
    }


    @GetMapping("/env")
    public String getEnvInfo() {
        return envService.envInfo();
    }


    @GetMapping("/app")
    public AppProperties getAppProperties() {
        return appProperties;
    }
}
