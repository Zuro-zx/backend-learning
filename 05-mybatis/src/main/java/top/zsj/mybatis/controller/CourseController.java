package top.zsj.mybatis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.mybatis.entity.Course;
import top.zsj.mybatis.service.CourseService;

import java.util.List;

/**
 * 课程接口，路径前缀 /courses
 *
 * @author zsj
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /* 增 */
    @PostMapping
    public Course create(@RequestBody Course course) {
        courseService.createCourse(course);
        return course;
    }

    @PostMapping("/batch")
    public int createBatch(@RequestBody List<Course> courses) {
        courseService.createCourseBatch(courses);
        return courses.size();
    }

    /* 查 */
    @GetMapping("/{id}")
    public Course getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @GetMapping
    public List<Course> list() {
        return courseService.list();
    }

    /* 删 */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return courseService.removeById(id);
    }

    @DeleteMapping("/batch")
    public boolean deleteBatch(@RequestParam List<Long> ids) {
        return courseService.removeCourseBatch(ids);
    }
}
