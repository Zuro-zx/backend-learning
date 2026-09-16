package top.mqxu.config.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/9
 * @description
 */
@RestController
@RequestMapping("/config")
public class Configcontroller {

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${mqxu.name}")
    private String myname;

    @Value("${mqxu.job}")
    private String myjob;


    @GetMapping("/basic")
    public String getBasicInfo(){
        return "服务器端口是：" + this.serverPort + ",应用名称是：" + appName;
    }

    @GetMapping("/my")
    public String getMyInfo(){
        return "我的姓名是：" + this.myname+ ",我的职业是：" + myjob;
    }
}