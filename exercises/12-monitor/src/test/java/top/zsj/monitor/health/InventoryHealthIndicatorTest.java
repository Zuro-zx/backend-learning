package top.zsj.monitor.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import top.zsj.monitor.inventory.InventoryStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InventoryHealthIndicator} 的单元测试。
 * <p>健康检查最怕的是「判定规则写反了」，而这种错误在集成测试里很难发现
 * （因为默认数据永远是健康的），所以规则本身要在单元测试里穷举。</p>
 *
 * @author zsj
 */
@DisplayName("InventoryHealthIndicator：只有归零才算 DOWN")
class InventoryHealthIndicatorTest {

    private InventoryStore inventoryStore;
    private InventoryHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        inventoryStore = new InventoryStore(5);
        indicator = new InventoryHealthIndicator(inventoryStore);
    }

    @Test
    @DisplayName("全部有货 → UP，details 里带上 SKU 数、最低库存、明细")
    void upWhenAllInStock() {
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("skuCount", 3L)
                .containsEntry("outOfStock", 0L)
                .containsEntry("minStock", 10);
        assertThat(health.getDetails().get("stocks"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("SKU-001", 50);
    }

    @Test
    @DisplayName("任何一个 SKU 归零 → DOWN")
    void downWhenAnySkuEmpty() {
        inventoryStore.setStock("SKU-002", 0);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("outOfStock", 1L)
                .containsEntry("minStock", 0);
    }

    @Test
    @DisplayName("低库存但没归零 → 仍然 UP，只是 lowStock 计数变大")
    void lowStockStillUp() {
        inventoryStore.setStock("SKU-003", 2);

        Health health = indicator.health();

        assertThat(health.getStatus())
                .as("低于预警线只该触发告警，不该把实例摘出负载均衡")
                .isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("lowStock", 1L)
                .containsEntry("lowStockThreshold", 5);
    }
}
