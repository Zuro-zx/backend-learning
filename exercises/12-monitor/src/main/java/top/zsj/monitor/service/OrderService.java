package top.zsj.monitor.service;

import org.springframework.stereotype.Service;
import top.zsj.monitor.common.BusinessException;
import top.zsj.monitor.common.ErrorCode;
import top.zsj.monitor.dto.OrderRequest;
import top.zsj.monitor.dto.OrderResult;
import top.zsj.monitor.inventory.InventoryStore;
import top.zsj.monitor.metrics.BusinessMetrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 下单。
 * <p>埋点写在这里而不是 Controller：Controller 只管协议，业务成功与否由 Service 判定，
 * 指标应该跟着业务判定走。</p>
 * <p>耗时统计用 {@code try/finally} 而不是只在成功分支记——<b>失败的耗时同样是信号</b>，
 * 如果只记成功，一次「大量请求全在 3 秒后失败」的故障在看板上会完全消失。</p>
 *
 * @author zsj
 */
@Service
public class OrderService {

    private final InventoryStore inventoryStore;
    private final BusinessMetrics metrics;
    private final AtomicLong sequence = new AtomicLong();

    public OrderService(InventoryStore inventoryStore, BusinessMetrics metrics) {
        this.inventoryStore = inventoryStore;
        this.metrics = metrics;
    }

    public OrderResult place(OrderRequest request) {
        long start = System.nanoTime();
        boolean success = false;
        try {
            if (!inventoryStore.deduct(request.sku(), request.quantity())) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                        "库存不足：需要 " + request.quantity() + " 件，仅剩 "
                                + inventoryStore.stockOf(request.sku()) + " 件");
            }
            success = true;
            return new OrderResult("NO-" + sequence.incrementAndGet(), request.sku(),
                    request.quantity(), request.amount(),
                    inventoryStore.stockOf(request.sku()));
        } finally {
            metrics.recordOrder(request.amount(), System.nanoTime() - start, success);
        }
    }
}
