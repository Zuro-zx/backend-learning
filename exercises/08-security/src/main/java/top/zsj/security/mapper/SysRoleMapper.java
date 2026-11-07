package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.zsj.security.entity.SysRole;

import java.util.List;

/**
 * 角色与权限点表
 *
 * @author zsj
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查某个账号的全部角色与权限点 */
    @Select("SELECT * FROM sec_role WHERE user_id = #{userId}")
    List<SysRole> findByUserId(@Param("userId") Long userId);
}
