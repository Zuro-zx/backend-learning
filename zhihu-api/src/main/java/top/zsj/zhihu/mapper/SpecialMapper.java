package top.zsj.zhihu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.zsj.zhihu.dto.CategoryCount;
import top.zsj.zhihu.entity.Special;

import java.util.List;

/**
 * 专题 Mapper，继承 BaseMapper 即拥有单表增删改查
 * <p>
 * 分类筛选与分页都由 LambdaQueryWrapper 拼装，只有「按分类聚合」这种
 * 单表 CRUD 表达不了的统计才写注解 SQL，避免维护一份 XML。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题 Mapper
 */
@Mapper
public interface SpecialMapper extends BaseMapper<Special> {

    /**
     * 按分类聚合，分类为空的历史数据不计入
     *
     * @return 每个分类及其专题数量，按数量倒序
     */
    @Select("""
            SELECT category, COUNT(*) AS total
            FROM t_special
            WHERE category IS NOT NULL
            GROUP BY category
            ORDER BY COUNT(*) DESC, category ASC
            """)
    List<CategoryCount> selectCategoryStats();
}
