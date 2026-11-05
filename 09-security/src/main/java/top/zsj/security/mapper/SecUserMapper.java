package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.zsj.security.entity.SecUser;

/**
 * 账号 Mapper
 *
 * @author zsj
 */
@Mapper
@Repository
public interface SecUserMapper extends BaseMapper<SecUser> {

    /** 按登录名查账号，登录与重名校验都用它 */
    @Select("SELECT * FROM sec_user WHERE username = #{username}")
    SecUser selectByUsername(@Param("username") String username);
}
