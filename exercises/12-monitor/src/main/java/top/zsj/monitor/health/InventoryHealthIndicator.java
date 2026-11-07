package top.zsj.monitor.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import top.zsj.monitor.inventory.InventoryStore;

import java.util.Map;

/**
 * 自定义健康检查：库存。
 * <p>Bean 名就是 {@code /actuator/health} 里 components 的键名，所以这里显式写成
 * {@code @Component("inventory")}，对应 JSON 里的 {@code components.inventory}。</p>
 * <p>判定规则：<b>任何一个 SKU 归零就 DOWN</b>。为什么不是「低于预警线就 DOWN」？
 * 因为健康检查的语义是「还能不能对外提供服务」——低于预警线仍然能卖，只是该补货了，
 * 那种情况属于告警（Gauge + 告警规则），不该把实例从负载均衡里摘掉。</p>
 *
 * @author zsj
 */
@Component("inventory")
public class InventoryHealthIndicator implements HealthIndicator {

    private final InventoryStore inventoryStore;

    public InventoryHealthIndicator(InventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    @Override
    public Health health() {
        Map<String, Integer> stocks = inventoryStore.snapshot();
        long outOfStock = inventoryStore.outOfStockCount();

        Health.Builder builder = outOfStock > 0 ? Health.down() : Health.up();
        return builder
                .withDetail("skuCount", (long) stocks.size())
                .withDetail("outOfStock", outOfStock)
                .withDetail("lowStock", inventoryStore.lowStockCount())
                .withDetail("lowStockThreshold", inventoryStore.getLowStockThreshold())
                .withDetail("minStock", inventoryStore.minStock())
                .withDetail("stocks", stocks)
                .build();
    }
}
