package top.zsj.deploy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description DeployController
 */
@RestController
@RequestMapping("/deploy")
public class DeployController {

    @GetMapping("/hello")
    public String hello() {
        return "部署与 CI/CD 模块";
    }
}
