package top.zsj.security.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 批改入参（FR-09）
 * <p>
 * 分数必填，0 到 100 的整数；评语可空，不超过 500 字。
 *
 * @author zsj
 */
public record ReviewRequest(
        @NotNull(message = "分数必填")
        @Min(value = 0, message = "分数最低 0 分")
        @Max(value = 100, message = "分数最高 100 分")
        Integer score,

        @Size(max = 500, message = "评语不能超过 500 字")
        String comment) {
}
