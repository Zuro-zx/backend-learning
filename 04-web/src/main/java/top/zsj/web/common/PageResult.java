package top.zsj.web.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装
 * <p>
 * 列表接口直接返回 List 的话，前端不知道总共有多少条、能不能翻下一页，
 * 所以统一包成 {list, total, pageNum, pageSize}。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 分页结果封装
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;
    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private int pageNum;
    /** 每页条数 */
    private int pageSize;

    /**
     * 把「已切好的当前页数据 + 总数」包成分页结果。
     * 切片动作由 Service 完成（真实项目里这一步由数据库的 LIMIT / OFFSET 完成）。
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
