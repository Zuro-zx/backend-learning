package top.zsj.doc.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

/**
 * 分页结果
 *
 * @author zsj
 */
@Getter
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private final List<T> list;

    @Schema(description = "总条数", example = "5")
    private final long total;

    @Schema(description = "页码，从 1 开始", example = "1")
    private final int pageNum;

    @Schema(description = "每页条数", example = "3")
    private final int pageSize;

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
