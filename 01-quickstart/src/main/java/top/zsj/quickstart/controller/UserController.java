package top.mqxu.quickstart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.quickstart.entity.User;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;

/**
 * @author zsj
 * @date 2026/9/9
 * @description
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/info")
    private User getUserInfo() {
        return new User(1001L,"张世杰",LocalDate.of(2005,12,24));
    }
}