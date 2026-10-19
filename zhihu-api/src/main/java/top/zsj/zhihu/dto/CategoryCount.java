package top.zsj.zhihu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类统计，供前端下拉框直接展示「分类 + 条数」
 *
 * @author zsj
 * @date 2026/10/19
 * @description 分类统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCount {

    /** 分类名称 */
    private String category;

    /** 该分类下的专题数量 */
    private Long total;
}
