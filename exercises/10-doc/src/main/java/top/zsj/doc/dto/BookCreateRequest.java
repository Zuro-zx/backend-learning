package top.zsj.doc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import top.zsj.doc.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增图书请求
 * <p>
 * 校验注解会被 springdoc 读出来转成 schema 约束：{@code @NotBlank} 进 required 列表、
 * {@code @Size(max = 100)} 变 maxLength、{@code @Pattern} 变 pattern。
 * 规则只写一遍，改这里文档自动跟着变。
 *
 * @author zsj
 */
@Data
@Schema(description = "新增图书请求")
public class BookCreateRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名不能超过 100 字")
    @Schema(description = "书名", example = "重构：改善既有代码的设计",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 50, message = "作者名不能超过 50 字")
    @Schema(description = "作者", example = "Martin Fowler",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @NotBlank(message = "ISBN 不能为空")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN 必须是 13 位数字")
    @Schema(description = "13 位 ISBN", example = "9787115508645",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String isbn;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类", example = "TECH", requiredMode = Schema.RequiredMode.REQUIRED)
    private Category category;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "单价，单位元", example = "118.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    @Schema(description = "库存，默认 0", example = "15")
    private Integer stock;

    @Schema(description = "出版日期", example = "2019-05-01")
    private LocalDate publishDate;
}
