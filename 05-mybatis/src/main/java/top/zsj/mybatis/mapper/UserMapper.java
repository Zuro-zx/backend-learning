package top.zsj.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.zsj.mybatis.entity.User;

/**
 * 用户 Mapper
 * @author zsj
 */
@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {

    /** 连接池实验专用：SELECT SLEEP 让当前连接占用 N 秒，只支持 MySQL */
    @Select("SELECT SLEEP(#{seconds})")
    Integer sleep(@Param("seconds") int seconds);
}
