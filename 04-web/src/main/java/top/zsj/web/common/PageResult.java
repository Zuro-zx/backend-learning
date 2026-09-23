package top.zsj.web.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 * <p>
 * 列表接口直接返回 List 的话，前端不知道总共有多少条、能不能翻下一页，
 * 所以统一包成 {total, pages, pageNum, pageSize, list}。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 分页返回结构
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;
    /** 总页数 */
    private Integer pages;
    /** 当前页码，从 1 开始 */
    private Integer pageNum;
    /** 每页条数 */
    private Integer pageSize;
    /** 当前页数据 */
    private List<T> list;

    private PageResult(Long total, Integer pages, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    /**
     * 对一个完整列表做内存分页
     * <p>
     * 真实项目里这步由数据库的 LIMIT / OFFSET 完成（05-mybatis 会讲），
     * 这里先在内存里模拟，把「分页结果长什么样」先固定下来。
     *
     * @param all      完整数据
     * @param pageNum  页码，从 1 开始；非法值按 1 处理
     * @param pageSize 每页条数；非法值按 10 处理
     */
    public static <T> PageResult<T> of(List<T> all, Integer pageNum, Integer pageSize) {
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        List<T> source = (all == null) ? List.of() : all;

        long total = source.size();
        // 向上取整算总页数：11 条 / 每页 5 条 = 3 页
        int pages = (int) ((total + size - 1) / size);

        // 用 long 计算起点，避免 pageNum 很大时 (num - 1) * size 溢出成负数
        int from = (int) Math.min((long) (num - 1) * size, total);
        int to = (int) Math.min(from + (long) size, total);

        return new PageResult<>(total, pages, num, size, source.subList(from, to));
    }
}
