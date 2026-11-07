package top.zsj.test.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.zsj.test.client.InventoryClient;
import top.zsj.test.common.BusinessException;
import top.zsj.test.common.ErrorCode;
import top.zsj.test.dto.OrderRequest;
import top.zsj.test.dto.OrderResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 下单试算。
 * <p>整个模块的核心被测对象，全部业务规则集中在这一个方法里，分支点一共 5 个：</p>
 * <ol>
 *   <li>库存是否够</li>
 *   <li>是否满 300</li>
 *   <li>是否满 100</li>
 *   <li>是否金卡</li>
 *   <li>是否触发 0.01 的下限</li>
 * </ol>
 * <p>规则写死成常量而不是配置项，是为了让单元测试可以直接用 {@code BigDecimal} 断言到分。</p>
 *
 * @author zsj
 */
@Slf4j
@Service
public class OrderService {

    /** 满 300 减 40 */
    static final BigDecimal FULL_300 = new BigDecimal("300");
    static final BigDecimal REDUCE_40 = new BigDecimal("40");
    /** 满 100 减 10 */
    static final BigDecimal FULL_100 = new BigDecimal("100");
    static final BigDecimal REDUCE_10 = new BigDecimal("10");
    /** 金卡在满减之后再打 95 折 */
    static final BigDecimal GOLD_RATE = new BigDecimal("0.95");
    /** 应付金额的绝对下限 */
    static final BigDecimal MIN_PAYABLE = new BigDecimal("0.01");

    static final String RULE_FULL_300 = "满300减40";
    static final String RULE_FULL_100 = "满100减10";
    static final String RULE_GOLD = "金卡95折";
    static final String RULE_FLOOR = "触底0.01";

    private final InventoryClient inventoryClient;

    public OrderService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    /**
     * 计算一单的应付金额。
     *
     * @param request 试算请求
     * @return 试算结果，{@code payable} 恒大于等于 0.01
     * @throws BusinessException 购买数量超过可售库存时抛 {@link ErrorCode#OUT_OF_STOCK}
     */
    public OrderResult calculate(OrderRequest request) {
        // ① 外部依赖：库存。单元测试里这一行由 mock 决定返回值
        int stock = inventoryClient.stockOf(request.sku());
        if (request.quantity() > stock) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "库存不足：需要 " + request.quantity() + " 件，仅剩 " + stock + " 件");
        }

        List<String> rules = new ArrayList<>(3);

        // ② 先把金额规整到「分」，后面所有比较都基于这个值
        BigDecimal original = request.amount().setScale(2, RoundingMode.HALF_UP);

        // ③ 满减：两档互斥，命中高档就不再判低档
        BigDecimal reduce = BigDecimal.ZERO;
        if (original.compareTo(FULL_300) >= 0) {
            reduce = REDUCE_40;
            rules.add(RULE_FULL_300);
        } else if (original.compareTo(FULL_100) >= 0) {
            reduce = REDUCE_10;
            rules.add(RULE_FULL_100);
        }
        BigDecimal afterReduce = original.subtract(reduce);

        // ④ 金卡折扣：注意是「满减之后」再打折，不是对原价打折
        BigDecimal payable = afterReduce;
        if (request.goldCard()) {
            rules.add(RULE_GOLD);
            payable = afterReduce.multiply(GOLD_RATE);
        }

        // ⑤ 触底保护：0.01 打 95 折是 0.0095，必须「先比较、后取舍」才能命中，
        //    如果先 setScale 再判断，0.0095 已经被舍成 0.01，这个分支永远走不到
        if (payable.compareTo(MIN_PAYABLE) < 0) {
            payable = MIN_PAYABLE;
            rules.add(RULE_FLOOR);
        }
        payable = payable.setScale(2, RoundingMode.HALF_UP);

        // ⑥ 反推金卡折扣额，保证 原价 - 满减 - 金卡折扣 == 应付，账单永远平
        BigDecimal goldCardDiscount = request.goldCard()
                ? afterReduce.subtract(payable)
                : BigDecimal.ZERO;

        OrderResult result = new OrderResult(request.orderNo(), original, reduce,
                goldCardDiscount, payable, stock, List.copyOf(rules));
        log.info("试算 {}：原价 {}，满减 {}，金卡折扣 {}，应付 {}，规则 {}",
                result.orderNo(), original, reduce, goldCardDiscount, payable, rules);
        return result;
    }
}
