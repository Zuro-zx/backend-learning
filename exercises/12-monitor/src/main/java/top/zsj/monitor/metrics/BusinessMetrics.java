package top.zsj.monitor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import top.zsj.monitor.inventory.InventoryStore;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 业务指标。
 * <p>Micrometer 的四类仪表各用一次，都用得上，没有凑数的：</p>
 * <table border="1">
 *   <caption>四种仪表</caption>
 *   <tr><th>类型</th><th>指标名</th><th>为什么是它</th></tr>
 *   <tr><td>Counter</td><td>{@code biz.order.placed}</td><td>只增不减的累计数，用 {@code result} 标签区分成功/失败</td></tr>
 *   <tr><td>Gauge</td><td>{@code biz.inventory.low.stock}</td><td>可增可减的瞬时值，直接读 {@link InventoryStore} 的当前状态</td></tr>
 *   <tr><td>Timer</td><td>{@code biz.order.process}</td><td>既计时长又计次数，还能算分位</td></tr>
 *   <tr><td>DistributionSummary</td><td>{@code biz.order.amount}</td><td>不是时间的「分布」，比如金额</td></tr>
 * </table>
 * <p><b>关键约定：</b>业务指标统一加 {@code biz.} 前缀，和框架自带的 {@code http.server.requests}、
 * {@code jvm.memory.used} 分开，看板上不会混在一起。</p>
 * <p><b>取名避坑：</b>不要用 Prometheus 的保留后缀（{@code total / count / sum / max / min / bucket / created}）
 * 当指标名的最后一段。第一版把计数器叫 {@code biz.order.created}，结果在 {@code /actuator/prometheus}
 * 里变成了 {@code biz_order_total}——Micrometer 把 {@code created} 当成后缀剥掉了，
 * 于是指标名里"下单"这个语义凭空消失，还和别的 {@code biz_order_*} 指标撞在一起。
 * 现在改成 {@code biz.order.placed}，Prometheus 里是 {@code biz_order_placed_total}。</p>
 *
 * @author zsj
 */
@Component
public class BusinessMetrics {

    public static final String ORDER_PLACED = "biz.order.placed";
    public static final String ORDER_PROCESS = "biz.order.process";
    public static final String ORDER_AMOUNT = "biz.order.amount";
    public static final String LOW_STOCK = "biz.inventory.low.stock";

    /** 成功与失败各建一个 Counter，同名不同 tag —— 在指标系统里是同一个指标的两个维度 */
    private final Counter placedSuccess;
    private final Counter placedFailed;
    private final Timer orderProcess;
    private final DistributionSummary orderAmount;
    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry, InventoryStore inventoryStore) {
        this.registry = registry;

        this.placedSuccess = Counter.builder(ORDER_PLACED)
                .description("下单请求总数")
                .tag("result", "success")
                .register(registry);
        this.placedFailed = Counter.builder(ORDER_PLACED)
                .description("下单请求总数")
                .tag("result", "failed")
                .register(registry);

        this.orderProcess = Timer.builder(ORDER_PROCESS)
                .description("下单处理耗时")
                .publishPercentiles(0.5, 0.95)
                .register(registry);

        this.orderAmount = DistributionSummary.builder(ORDER_AMOUNT)
                .description("订单金额分布")
                .baseUnit("CNY")
                .publishPercentiles(0.5, 0.9)
                .register(registry);

        // Gauge 持有的是「对象 + 取值函数」，不是快照值：
        // 注册时不会调 lowStockCount()，是每次抓取才读一次，所以永远是最新值
        Gauge.builder(LOW_STOCK, inventoryStore, store -> (double) store.lowStockCount())
                .description("低于预警线的 SKU 数量")
                .register(registry);
    }

    /**
     * 记录一次下单。
     *
     * @param amount  订单金额
     * @param nanos   处理耗时（纳秒）
     * @param success 是否成功；失败只记 Counter 与 Timer，不记金额
     */
    public void recordOrder(BigDecimal amount, long nanos, boolean success) {
        if (success) {
            placedSuccess.increment();
            orderAmount.record(amount.doubleValue());
        } else {
            placedFailed.increment();
        }
        orderProcess.record(nanos, TimeUnit.NANOSECONDS);
    }

    /** 供测试与排障直接拿注册表。 */
    public MeterRegistry registry() {
        return registry;
    }
}
