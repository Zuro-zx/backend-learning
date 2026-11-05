package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.zsj.security.entity.SecRole;

import java.util.List;

/**
 * 角色 Mapper
 *
 * @author zsj
 */
@Mapper
@Repository
public interface SecRoleMapper extends BaseMapper<SecRole> {

    /** 按角色编码查 id，建账号关联时用 */
    @Select("SELECT id FROM sec_role WHERE code = #{code}")
    Long selectIdByCode(String code);

    /** 全部角色编码，启动自检或管理端展示用 */
    @Select("SELECT code FROM sec_role")
    List<String> selectAllCodes();
}
