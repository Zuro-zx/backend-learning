package top.zsj.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.zsj.test.client.InventoryClient;
import top.zsj.test.common.BusinessException;
import top.zsj.test.common.ErrorCode;
import top.zsj.test.dto.OrderRequest;
import top.zsj.test.dto.OrderResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 第一层：单元测试。
 * <p>不启动 Spring、不连数据库、不发 HTTP，<b>只</b>测 {@link OrderService} 一个类。
 * 外部依赖 {@link InventoryClient} 用 Mockito 的 {@code @Mock} 顶替，
 * 这样才能把「库存只有 0 件」这类现实中很难造的场景随手写出来。</p>
 * <p>{@code MockitoExtension} 默认 strict stubs：stub 了但没用上会直接报错，
 * 所以这里把 {@code stockOf} 的 stub 放在 {@code @BeforeEach}，保证每个用例都会真正用到它。</p>
 *
 * @author zsj
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试：只要一个类 + 一个 mock")
class OrderServiceTest {

    private static final String SKU = "SKU-DEFAULT";

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void stubInventory() {
        // 默认库存 100 件，所有用例都走这条 stub；要测库存不足就把数量调到 101
        when(inventoryClient.stockOf(anyString())).thenReturn(100);
    }

    // ---------- 满减规则 ----------

    @Test
    @DisplayName("满 300 减 40：300 元应付 260")
    void fullThreeHundred() {
        OrderResult result = orderService.calculate(req("300"));

        assertThat(result.originalAmount()).isEqualByComparingTo("300.00");
        assertThat(result.reduceAmount()).isEqualByComparingTo("40");
        assertThat(result.payable()).isEqualByComparingTo("260.00");
        assertThat(result.appliedRules()).containsExactly("满300减40");
    }

    @Test
    @DisplayName("满减边界：299.99 只够满 100 减 10")
    void justBelowThreeHundred() {
        OrderResult result = orderService.calculate(req("299.99"));

        assertThat(result.reduceAmount()).isEqualByComparingTo("10");
        assertThat(result.payable()).isEqualByComparingTo("289.99");
        assertThat(result.appliedRules()).containsExactly("满100减10");
    }

    @Test
    @DisplayName("满 100 减 10：100 元应付 90")
    void fullOneHundred() {
        OrderResult result = orderService.calculate(req("100"));

        assertThat(result.reduceAmount()).isEqualByComparingTo("10");
        assertThat(result.payable()).isEqualByComparingTo("90.00");
        assertThat(result.appliedRules()).containsExactly("满100减10");
    }

    @Test
    @DisplayName("不足 100 不减：99.99 原价就是应付")
    void belowOneHundred() {
        OrderResult result = orderService.calculate(req("99.99"));

        assertThat(result.reduceAmount()).isEqualByComparingTo("0");
        assertThat(result.payable()).isEqualByComparingTo("99.99");
        assertThat(result.appliedRules()).isEmpty();
    }

    // ---------- 金卡折扣（在满减之后） ----------

    @Test
    @DisplayName("金卡在满减之后打折：300 → (300-40)×0.95 = 247")
    void goldCardAfterReduce() {
        OrderResult result = orderService.calculate(req("300", true));

        assertThat(result.reduceAmount()).isEqualByComparingTo("40");
        assertThat(result.goldCardDiscount()).isEqualByComparingTo("13.00");
        assertThat(result.payable()).isEqualByComparingTo("247.00");
        assertThat(result.appliedRules()).containsExactly("满300减40", "金卡95折");
    }

    @Test
    @DisplayName("金卡 + 满 100 减 10：90×0.95 = 85.50")
    void goldCardWithReduceTen() {
        OrderResult result = orderService.calculate(req("100", true));

        assertThat(result.goldCardDiscount()).isEqualByComparingTo("4.50");
        assertThat(result.payable()).isEqualByComparingTo("85.50");
    }

    @Test
    @DisplayName("金卡但没满减：50×0.95 = 47.50")
    void goldCardOnly() {
        OrderResult result = orderService.calculate(req("50", true));

        assertThat(result.reduceAmount()).isEqualByComparingTo("0");
        assertThat(result.goldCardDiscount()).isEqualByComparingTo("2.50");
        assertThat(result.payable()).isEqualByComparingTo("47.50");
        assertThat(result.appliedRules()).containsExactly("金卡95折");
    }

    // ---------- 下限与取整 ----------

    @Test
    @DisplayName("应付触底：0.01 元打 95 折（0.0095）仍按 0.01 收")
    void payableFloor() {
        OrderResult result = orderService.calculate(req("0.01", true));

        assertThat(result.payable()).isEqualByComparingTo("0.01");
        // 折扣被下限吃掉，实际没收任何优惠，账单依然平
        assertThat(result.goldCardDiscount()).isEqualByComparingTo("0");
        assertThat(result.appliedRules()).containsExactly("金卡95折", "触底0.01");
    }

    @Test
    @DisplayName("金额先四舍五入到分：100.005 → 100.01，可享满 100 减 10")
    void roundingToCent() {
        OrderResult result = orderService.calculate(req("100.005"));

        assertThat(result.originalAmount()).isEqualByComparingTo("100.01");
        assertThat(result.payable()).isEqualByComparingTo("90.01");
    }

    @Test
    @DisplayName("账单恒等式：原价 - 满减 - 金卡折扣 == 应付")
    void amountsAlwaysBalance() {
        OrderResult result = orderService.calculate(req("399.99", true));

        BigDecimal check = result.originalAmount()
                .subtract(result.reduceAmount())
                .subtract(result.goldCardDiscount());
        assertThat(check).isEqualByComparingTo(result.payable());
        assertThat(result.payable()).isEqualByComparingTo("341.99");
    }

    // ---------- 库存 ----------

    @Test
    @DisplayName("库存不足：要 101 件只剩 100 件，抛 OUT_OF_STOCK")
    void outOfStock() {
        OrderRequest request = new OrderRequest("NO-OOS", new BigDecimal("100"), false, SKU, 101);

        assertThatThrownBy(() -> orderService.calculate(request))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);
                    assertThat(e.getMessage()).contains("库存不足").contains("101").contains("100");
                });
    }

    @Test
    @DisplayName("库存刚好够：要 100 件不报错，库存原样返回")
    void stockExactlyEnough() {
        OrderResult result = orderService.calculate(
                new OrderRequest("NO-OK", new BigDecimal("100"), false, SKU, 100));

        assertThat(result.availableStock()).isEqualTo(100);
        assertThat(result.payable()).isEqualByComparingTo("90.00");
    }

    // ---------- 默认值与依赖交互 ----------

    @Test
    @DisplayName("sku 全空白时用 SKU-DEFAULT 去查库存")
    void blankSkuFallsBackToDefault() {
        orderService.calculate(new OrderRequest("NO-SKU", new BigDecimal("100"), false, "   ", 1));

        verify(inventoryClient).stockOf("SKU-DEFAULT");
    }

    @Test
    @DisplayName("紧凑构造器补默认值：goldCard=false、quantity=1、sku=SKU-DEFAULT")
    void compactConstructorDefaults() {
        OrderRequest request = new OrderRequest("NO-DEF", new BigDecimal("50"), null, null, null);

        assertThat(request.goldCard()).isFalse();
        assertThat(request.quantity()).isEqualTo(1);
        assertThat(request.sku()).isEqualTo("SKU-DEFAULT");

        // 默认值也要真的能算出结果，而不是只有字段好看
        assertThat(orderService.calculate(request).payable()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("quantity 传 0 或负数都按 1 处理")
    void nonPositiveQuantityFallsBackToOne() {
        OrderRequest zero = new OrderRequest("NO-Z", new BigDecimal("50"), false, SKU, 0);
        OrderRequest negative = new OrderRequest("NO-N", new BigDecimal("50"), false, SKU, -3);

        assertThat(zero.quantity()).isEqualTo(1);
        assertThat(negative.quantity()).isEqualTo(1);
        assertThat(orderService.calculate(zero).payable()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("库存只查一次，不做重复远程调用")
    void stockQueriedExactlyOnce() {
        orderService.calculate(req("100"));

        verify(inventoryClient, times(1)).stockOf(SKU);
    }

    // ---------- 构造请求的小工具 ----------

    private OrderRequest req(String amount) {
        return req(amount, false);
    }

    private OrderRequest req(String amount, boolean goldCard) {
        return new OrderRequest("NO-TEST", new BigDecimal(amount), goldCard, SKU, 1);
    }
}
