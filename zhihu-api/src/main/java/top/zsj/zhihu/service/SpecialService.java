package top.zsj.zhihu.service;

import top.zsj.zhihu.common.PageResult;
import top.zsj.zhihu.dto.CategoryCount;
import top.zsj.zhihu.entity.Special;

import java.util.List;

/**
 * 专题业务接口
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题业务接口
 */
public interface SpecialService {

    /**
     * 获取所有专题，按更新时间倒序
     *
     * @return 专题列表
     */
    List<Special> getAll();

    /**
     * 按分类获取专题（扩展练习 2）
     *
     * @param category 分类名称，传空或「全部」时等价于查全部
     * @return 该分类下的专题列表
     */
    List<Special> getByCategory(String category);

    /**
     * 按 limit / offset 分页获取专题（扩展练习 3）
     * <p>
     * 用 offset 而不是页码，是为了和知乎原始接口（limit=10&amp;offset=xxx）保持一致，
     * 后续若要直接转发上游分页参数也不用换算。
     *
     * @param limit    每页条数，调用方需保证已在合法区间
     * @param offset   起始偏移量，调用方需保证非负
     * @param category 分类，为空表示全部分类
     * @return 分页结果，含总数与上下页标记
     */
    PageResult<Special> getByPage(int limit, int offset, String category);

    /**
     * 分类及各自条数，供前端下拉框渲染
     *
     * @return 分类统计列表
     */
    List<CategoryCount> getCategoryStats();
}
