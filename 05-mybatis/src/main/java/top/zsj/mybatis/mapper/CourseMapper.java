package top.zsj.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.zsj.mybatis.entity.Course;

/**
 * 课程 Mapper
 *
 * @author zsj
 */
@Mapper
@Repository
public interface CourseMapper extends BaseMapper<Course> {
}
