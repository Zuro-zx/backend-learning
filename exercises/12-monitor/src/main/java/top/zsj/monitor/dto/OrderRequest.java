package top.zsj.monitor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 下单请求。
 *
 * @param sku      商品编码
 * @param quantity 数量
 * @param amount   金额
 * @author zsj
 */
public record OrderRequest(
        @NotBlank(message = "sku 不能为空") String sku,
        @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为 1") Integer quantity,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额不能小于 0.01") BigDecimal amount) {
}
