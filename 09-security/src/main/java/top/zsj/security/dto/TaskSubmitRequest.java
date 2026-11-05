package top.zsj.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交任务入参（FR-04）
 * <p>
 * 标题非空且不超过 100 字，内容非空且不超过 2000 字。
 * 不收 owner 字段：提交人取自令牌，请求体里写 owner 不生效（BR-1）。
 *
 * @author zsj
 */
public record TaskSubmitRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过 100 字")
        String title,

        @NotBlank(message = "内容不能为空")
        @Size(max = 2000, message = "内容不能超过 2000 字")
        String content) {
}
