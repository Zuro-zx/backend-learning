package top.zsj.test.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算结果。
 * <p>字段拆得细，是为了让断言可以精确到「哪一步省了多少钱」，
 * 而不是只断言一个最终应付金额——那样出问题时根本看不出是哪条规则错了。</p>
 *
 * @param orderNo          订单号
 * @param originalAmount   原始金额（已四舍五入到分）
 * @param reduceAmount     满减金额
 * @param goldCardDiscount 金卡折扣金额
 * @param payable          应付金额，恒 >= 0.01
 * @param availableStock   当前库存（来自 InventoryClient，单元测试里被 mock）
 * @param appliedRules     实际命中的规则，按生效顺序排列
 * @author zsj
 */
public record OrderResult(
        String orderNo,
        BigDecimal originalAmount,
        BigDecimal reduceAmount,
        BigDecimal goldCardDiscount,
        BigDecimal payable,
        int availableStock,
        List<String> appliedRules) {
}
