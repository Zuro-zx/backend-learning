package top.zsj.doc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.zsj.doc.entity.Category;

/**
 * 分页查询条件
 * <p>
 * 用对象接查询参数时，控制器上必须加 {@code @ParameterObject}：
 * 不加的话，文档里会把整个对象显示成一个叫 query 的参数，前端照着拼
 * {@code ?query={"pageNum":1}} 服务端收不到。
 *
 * @author zsj
 */
@Data
@Schema(description = "图书分页查询条件")
public class BookQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "关键字，匹配书名或作者", example = "Java")
    private String keyword;

    @Schema(description = "分类过滤", example = "TECH")
    private Category category;
}
