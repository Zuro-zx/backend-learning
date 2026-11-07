package top.zsj.monitor.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.zsj.monitor.inventory.InventoryStore;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BusinessMetrics} 的单元测试。
 * <p>用 {@code SimpleMeterRegistry} 而不是 Mockito：指标系统是有状态的，
 * 用真注册表才能验出「记了没记、记了几次、值对不对」，用 mock 只能验「方法被调了」。</p>
 * <p>这也是单元测试的一个判据：<b>被测对象的核心价值就是它的状态变化时，不要 mock 它</b>。</p>
 *
 * @author zsj
 */
@DisplayName("BusinessMetrics：四种仪表各自记对")
class BusinessMetricsTest {

    private SimpleMeterRegistry registry;
    private InventoryStore inventoryStore;
    private BusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        inventoryStore = new InventoryStore(5);
        metrics = new BusinessMetrics(registry, inventoryStore);
    }

    @Test
    @DisplayName("Counter：成功一次，result=success 的计数加一")
    void successIncrementsCounter() {
        metrics.recordOrder(new BigDecimal("129.00"), 1_000_000L, true);

        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "failed").counter().count()).isZero();
    }

    @Test
    @DisplayName("Counter：失败记在 result=failed 上，不污染成功计数")
    void failureUsesSeparateTag() {
        metrics.recordOrder(new BigDecimal("99.00"), 2_000_000L, false);
        metrics.recordOrder(new BigDecimal("99.00"), 2_000_000L, false);

        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "failed").counter().count()).isEqualTo(2.0);
        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "success").counter().count()).isZero();
    }

    @Test
    @DisplayName("DistributionSummary：金额落进分布，count 与 total 都对")
    void amountRecordedInSummary() {
        metrics.recordOrder(new BigDecimal("100.00"), 1_000_000L, true);
        metrics.recordOrder(new BigDecimal("300.00"), 1_000_000L, true);

        var summary = registry.get(BusinessMetrics.ORDER_AMOUNT).summary();
        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.totalAmount()).isEqualTo(400.0);
        assertThat(summary.max()).isEqualTo(300.0);
        assertThat(summary.getId().getBaseUnit()).isEqualTo("CNY");
    }

    @Test
    @DisplayName("DistributionSummary：失败的订单不记金额，否则金额分布会被脏数据带偏")
    void failureDoesNotRecordAmount() {
        metrics.recordOrder(new BigDecimal("100.00"), 1_000_000L, false);

        assertThat(registry.get(BusinessMetrics.ORDER_AMOUNT).summary().count()).isZero();
    }

    @Test
    @DisplayName("Timer：成功和失败都计时，因为「失败的耗时」同样是信号")
    void timerRecordsBothOutcomes() {
        metrics.recordOrder(new BigDecimal("100.00"), 5_000_000L, true);
        metrics.recordOrder(new BigDecimal("100.00"), 8_000_000L, false);

        var timer = registry.get(BusinessMetrics.ORDER_PROCESS).timer();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.max(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Gauge：注册时读的是对象引用，库存一变指标立刻跟着变")
    void gaugeFollowsInventory() {
        // 种子数据 50 / 30 / 10，预警线 5，所以初始是 0 个低库存
        assertThat(registry.get(BusinessMetrics.LOW_STOCK).gauge().value()).isZero();

        inventoryStore.setStock("SKU-001", 3);
        inventoryStore.setStock("SKU-002", 1);
        assertThat(registry.get(BusinessMetrics.LOW_STOCK).gauge().value()).isEqualTo(2.0);

        inventoryStore.setStock("SKU-001", 50);
        inventoryStore.setStock("SKU-002", 30);
        assertThat(registry.get(BusinessMetrics.LOW_STOCK).gauge().value()).isZero();
    }

    @Test
    @DisplayName("同名指标只注册一次：反复记录不会往注册表里新塞 meter")
    void metersRegisteredOnce() {
        int before = registry.getMeters().size();

        metrics.recordOrder(new BigDecimal("10.00"), 1_000L, true);
        metrics.recordOrder(new BigDecimal("10.00"), 1_000L, true);
        metrics.recordOrder(new BigDecimal("10.00"), 1_000L, false);

        // 注意：注册表里的 meter 数不止 5 个 —— publishPercentiles 会为每个分位额外注册一个 Gauge，
        // 所以这里断言的是「数量没变」，而不是「等于某个固定值」
        assertThat(registry.getMeters()).hasSize(before);
        assertThat(registry.get(BusinessMetrics.ORDER_PLACED).meters()).hasSize(2);
    }
}
