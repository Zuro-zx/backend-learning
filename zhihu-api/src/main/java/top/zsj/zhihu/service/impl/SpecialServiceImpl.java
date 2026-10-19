package top.zsj.zhihu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.zsj.zhihu.common.PageResult;
import top.zsj.zhihu.dto.CategoryCount;
import top.zsj.zhihu.entity.Special;
import top.zsj.zhihu.mapper.SpecialMapper;
import top.zsj.zhihu.service.SpecialService;

import java.util.List;

/**
 * 专题业务实现
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题业务实现
 */
@Service
public class SpecialServiceImpl implements SpecialService {

    /** 前端下拉框的「全部」选项，语义上等同不过滤 */
    private static final String ALL = "全部";

    @Resource
    private SpecialMapper specialMapper;

    @Override
    public List<Special> getAll() {
        return specialMapper.selectList(orderByUpdated(filter(null)));
    }

    @Override
    public List<Special> getByCategory(String category) {
        return specialMapper.selectList(orderByUpdated(filter(category)));
    }

    @Override
    public PageResult<Special> getByPage(int limit, int offset, String category) {
        Long total = specialMapper.selectCount(filter(category));

        // offset 已越过末尾时直接给空列表，不必再查一次库
        List<Special> records = offset >= total
                ? List.of()
                : specialMapper.selectList(orderByUpdated(filter(category))
                        // limit / offset 在 Controller 已收敛为 1~50、>=0 的 int，
                        // 拼进 SQL 不存在注入面；这样能严格按 offset 语义取数，
                        // 不受「页码 × 每页条数」必须整除的限制
                        .last("LIMIT " + limit + " OFFSET " + offset));

        return PageResult.<Special>builder()
                .records(records)
                .total(total)
                .limit(limit)
                .offset(offset)
                .hasNext(offset + records.size() < total)
                .hasPrev(offset > 0)
                .build();
    }

    @Override
    public List<CategoryCount> getCategoryStats() {
        return specialMapper.selectCategoryStats();
    }

    /**
     * 组装过滤条件：分类为空或「全部」时不过滤
     *
     * @param category 分类名称
     * @return 查询条件
     */
    private LambdaQueryWrapper<Special> filter(String category) {
        LambdaQueryWrapper<Special> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category) && !ALL.equals(category.trim())) {
            wrapper.eq(Special::getCategory, category.trim());
        }
        return wrapper;
    }

    /**
     * 统一排序：更新时间倒序，并以 id 倒序兜底
     * <p>
     * 同一批导入的数据 updated 大量重复，只按 updated 排序时 MySQL 不保证跨页顺序稳定，
     * 翻页会出现同一条记录重复出现或被跳过。
     *
     * @param wrapper 已带过滤条件的 wrapper
     * @return 追加排序后的 wrapper
     */
    private LambdaQueryWrapper<Special> orderByUpdated(LambdaQueryWrapper<Special> wrapper) {
        return wrapper.orderByDesc(Special::getUpdated).orderByDesc(Special::getId);
    }
}
