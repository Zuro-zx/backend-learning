package top.zsj.file.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zsj
 * @date 2026/9/23
 * @description FileController
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @GetMapping("/hello")
    public String hello() {
        return "文件处理模块";
    }
}
