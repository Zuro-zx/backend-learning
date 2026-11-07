package top.zsj.monitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.monitor.common.Result;
import top.zsj.monitor.inventory.InventoryStore;

import java.util.Map;

/**
 * 库存管理接口。
 * <p>存在的意义是让「库存变化 → Gauge 变化 / 健康状态变化」这条链路可以被手工验证，
 * 真实项目里这会是后台管理端的一小块。</p>
 *
 * @author zsj
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryStore inventoryStore;

    public InventoryController(InventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    @GetMapping
    public Result<Map<String, Integer>> all() {
        return Result.ok(inventoryStore.snapshot());
    }

    @PostMapping("/{sku}/stock")
    public Result<Integer> setStock(@PathVariable String sku, @RequestParam int value) {
        inventoryStore.setStock(sku, value);
        return Result.ok(inventoryStore.stockOf(sku));
    }
}
