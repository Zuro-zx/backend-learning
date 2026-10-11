package top.zsj.mybatis.service;

import com.baomidou.mybatisplus.spring.service.IService;
import top.zsj.mybatis.entity.Course;

import java.util.List;

/**
 * 课程业务接口，练习批量保存与批量删除
 *
 * @author zsj
 */
public interface CourseService extends IService<Course> {

    boolean createCourse(Course course);

    boolean createCourseBatch(List<Course> courses);

    boolean removeCourseBatch(List<Long> ids);
}
