package top.zsj.doc.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图书分类
 * <p>
 * 每个枚举值加 {@code @Schema} 后，UI 里就是带说明的下拉框；不加只显示裸的 TECH、LITERATURE。
 *
 * @author zsj
 */
@Schema(description = "图书分类")
public enum Category {

    @Schema(description = "计算机与技术")
    TECH,

    @Schema(description = "文学")
    LITERATURE,

    @Schema(description = "历史")
    HISTORY,

    @Schema(description = "科普")
    SCIENCE,

    @Schema(description = "其他")
    OTHER
}
