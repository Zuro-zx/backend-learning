package top.zsj.doc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description DocController
 */
@RestController
@RequestMapping("/doc")
public class DocController {

    @GetMapping("/hello")
    public String hello() {
        return "接口文档模块";
    }
}
