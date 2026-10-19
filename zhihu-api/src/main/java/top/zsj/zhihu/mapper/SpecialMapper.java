package top.zsj.zhihu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.zsj.zhihu.entity.Special;

/**
 * 专题 Mapper，继承 BaseMapper 即拥有单表增删改查
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题 Mapper
 */
@Mapper
public interface SpecialMapper extends BaseMapper<Special> {

}
