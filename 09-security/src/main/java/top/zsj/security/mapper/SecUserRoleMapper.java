package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.zsj.security.entity.SecUserRole;

import java.util.List;

/**
 * 账号-角色关联 Mapper
 *
 * @author zsj
 */
@Mapper
@Repository
public interface SecUserRoleMapper extends BaseMapper<SecUserRole> {

    /** 查某账号的全部角色编码，联表用注解 SQL，SQL 始终在自己手里 */
    @Select("""
            SELECT r.code
            FROM sec_role r
            JOIN sec_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /** 删账号前先删关联：DELETE FROM sec_user_role WHERE user_id = #{userId} */
    @Delete("DELETE FROM sec_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
