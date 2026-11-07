package top.zsj.test.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 下单试算请求。
 * <p>用 record 是因为它天然不可变、equals/hashCode 现成，写测试时构造数据最省事。</p>
 * <p><b>注意：紧凑构造器在参数校验之前执行</b>，所以能在这里补默认值的字段（goldCard / sku / quantity）
 * 就不要加 {@code @NotNull}，否则注解永远不会触发。</p>
 *
 * @param orderNo  订单号，必填
 * @param amount   订单金额，必填且不小于 0.01
 * @param goldCard 是否金卡，缺省 false
 * @param sku      商品编码，缺省 SKU-DEFAULT
 * @param quantity 购买数量，缺省 1
 * @author zsj
 */
public record OrderRequest(
        @NotBlank(message = "订单号不能为空") String orderNo,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额不能小于 0.01") BigDecimal amount,
        Boolean goldCard,
        String sku,
        Integer quantity) {

    public OrderRequest {
        goldCard = Boolean.TRUE.equals(goldCard);
        sku = (sku == null || sku.isBlank()) ? "SKU-DEFAULT" : sku.trim();
        quantity = (quantity == null || quantity < 1) ? 1 : quantity;
    }

    /** 给 GET 试算接口用：只关心金额和卡种，其余走默认值。 */
    public static OrderRequest of(BigDecimal amount, Boolean goldCard) {
        return new OrderRequest("QUICK", amount, goldCard, null, null);
    }
}
