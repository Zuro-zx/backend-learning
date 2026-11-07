package top.zsj.monitor.dto;

import java.math.BigDecimal;

/**
 * 下单结果。
 *
 * @param orderNo        订单号
 * @param sku            商品编码
 * @param quantity       数量
 * @param amount         金额
 * @param remainingStock 扣减后的剩余库存
 * @author zsj
 */
public record OrderResult(
        String orderNo,
        String sku,
        int quantity,
        BigDecimal amount,
        int remainingStock) {
}
