package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.zsj.security.entity.SysUser;

/**
 * 账号表
 *
 * @author zsj
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 按登录名查人（密码登录） */
    @Select("SELECT * FROM sec_user WHERE username = #{username} LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    /** 按手机号查人（短信登录） */
    @Select("SELECT * FROM sec_user WHERE phone = #{phone} LIMIT 1")
    SysUser findByPhone(@Param("phone") String phone);
}
