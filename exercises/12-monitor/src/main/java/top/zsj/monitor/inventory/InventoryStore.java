package top.zsj.monitor.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存版库存表。
 * <p>这个类同时被三处消费，正好演示「同一份业务状态可以派生出不同视角的监控」：</p>
 * <ul>
 *   <li>{@code BusinessMetrics} 把「低于预警线的 SKU 数」做成 Gauge</li>
 *   <li>{@code InventoryHealthIndicator} 把「有没有 SKU 归零」做成健康状态</li>
 *   <li>{@code OrderService} 扣减库存</li>
 * </ul>
 *
 * @author zsj
 */
@Component
public class InventoryStore {

    private final int lowStockThreshold;
    private final Map<String, Integer> stocks = new ConcurrentHashMap<>();

    public InventoryStore(@Value("${app.inventory.low-stock-threshold:5}") int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
        // 三个种子 SKU：最后一个 10 件，演示「接近预警线但还没到」
        stocks.put("SKU-001", 50);
        stocks.put("SKU-002", 30);
        stocks.put("SKU-003", 10);
    }

    public int stockOf(String sku) {
        return stocks.getOrDefault(sku, 0);
    }

    /** 直接设定库存（管理接口 + 测试用）。 */
    public void setStock(String sku, int stock) {
        stocks.put(sku, Math.max(stock, 0));
    }

    /**
     * 扣减库存。
     *
     * @return true 表示扣减成功；库存不足时原样保留并返回 false
     */
    public boolean deduct(String sku, int quantity) {
        AtomicBoolean success = new AtomicBoolean(false);
        stocks.compute(sku, (key, current) -> {
            int available = current == null ? 0 : current;
            if (available >= quantity) {
                success.set(true);
                return available - quantity;
            }
            return available;
        });
        return success.get();
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(stocks);
    }

    public int skuCount() {
        return stocks.size();
    }

    /** 低于预警线的 SKU 数量，Gauge 的数据源。 */
    public long lowStockCount() {
        return stocks.values().stream().filter(stock -> stock < lowStockThreshold).count();
    }

    /** 已归零的 SKU 数量，健康检查用。 */
    public long outOfStockCount() {
        return stocks.values().stream().filter(stock -> stock <= 0).count();
    }

    public int minStock() {
        return stocks.values().stream().mapToInt(Integer::intValue).min().orElse(0);
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }
}
