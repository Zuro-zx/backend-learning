package top.mqxu.config.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.config.properties.StudentProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生配置接口
 *
 * <p>把 application.yml 中 student 前缀的配置通过接口暴露出来，
 * 用于验证 @ConfigurationProperties 各类结构的绑定结果。</p>
 *
 * @author zsj
 * @date 2026/9/16
 * @description 配置绑定结果查询接口
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    private final StudentProperties studentProperties;

    public StudentController(StudentProperties studentProperties) {
        this.studentProperties = studentProperties;
    }

    /**
     * 查询学生完整配置信息
     *
     * @return 包含全部配置结构的对象
     */
    @GetMapping("/info")
    public StudentProperties getStudentInfo() {
        return studentProperties;
    }

    /**
     * 查询简单字段配置
     *
     * @return 姓名与年龄
     */
    @GetMapping("/basic")
    public Map<String, Object> getBasicInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", studentProperties.getName());
        result.put("age", studentProperties.getAge());
        return result;
    }

    /**
     * 查询 List 类型配置
     *
     * @return 爱好列表
     */
    @GetMapping("/hobbies")
    public List<String> getHobbies() {
        return studentProperties.getHobbies();
    }

    /**
     * 查询 Map 类型配置
     *
     * @return 各科目成绩
     */
    @GetMapping("/scores")
    public Map<String, Integer> getScores() {
        return studentProperties.getScores();
    }

    /**
     * 查询嵌套对象配置
     *
     * @return 家庭住址
     */
    @GetMapping("/address")
    public StudentProperties.Address getAddress() {
        return studentProperties.getAddress();
    }

    /**
     * 查询 List 嵌套对象配置
     *
     * @return 课程列表
     */
    @GetMapping("/courses")
    public List<StudentProperties.Course> getCourses() {
        return studentProperties.getCourses();
    }
}
