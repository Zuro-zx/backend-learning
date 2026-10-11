package top.zsj.mybatis.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zsj.mybatis.entity.Course;
import top.zsj.mybatis.mapper.CourseMapper;
import top.zsj.mybatis.service.CourseService;

import java.util.List;

/**
 * 课程业务实现
 *
 * @author zsj
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private static final int BATCH_SIZE = 100;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createCourse(Course course) {
        return save(course);
    }

    /**
     * saveBatch 按批次大小分批提交，默认 1000，这里改成 100 演示自定义批次
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createCourseBatch(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            throw new IllegalArgumentException("批量新增的集合不能为空");
        }
        return saveBatch(courses, BATCH_SIZE);
    }

    /**
     * 实体带 @TableLogic，removeByIds 实际执行的是 UPDATE deleted = 1
     */
    @Override
    public boolean removeCourseBatch(List<Long> ids) {
        return removeByIds(ids);
    }
}
