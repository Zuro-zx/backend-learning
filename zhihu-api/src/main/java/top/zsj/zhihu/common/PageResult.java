package top.zsj.zhihu.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果
 * <p>
 * 分页接口沿用知乎原始接口的 limit / offset 语义（而不是页码），
 * 因此返回体里同时给出 total、hasNext、hasPrev，前端不必自己推算边界。
 *
 * @param <T> 记录类型
 * @author zsj
 * @date 2026/10/19
 * @description 分页结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 满足条件的总条数（不受 limit 限制） */
    private Long total;

    /** 每页条数 */
    private Integer limit;

    /** 起始偏移量 */
    private Integer offset;

    /** 是否还有下一页 */
    private Boolean hasNext;

    /** 是否为第一页 */
    private Boolean hasPrev;
}
