package top.zsj.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.zsj.monitor.metrics.BusinessMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actuator 端点的集成测试。
 * <p>监控这种能力有个特点：<b>代码写对了不等于端点能访问</b>。
 * 少配一个 {@code management.endpoints.web.exposure.include}、少引一个 registry 依赖、
 * 大小写写错一个指标名，都只有在真启动之后才能发现。所以这一层必须用 {@code @SpringBootTest}。</p>
 * <p>另外注意断言方式：涉及累计值的断言一律用「相对增减」而不是绝对值，
 * 因为同一个 Spring 上下文被所有用例共享，Counter 是单调累加的，写死绝对值必然翻车。</p>
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator 端点：健康、信息、指标、Prometheus 全链路")
class MonitorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry registry;

    // ---------- health ----------

    @Test
    @DisplayName("/actuator/health：整体 UP，且自定义的 inventory 组件在里面")
    void healthShowsCustomComponent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.inventory.status").value("UP"))
                // show-details: always 才会出现 details
                .andExpect(jsonPath("$.components.inventory.details.skuCount").value(3))
                .andExpect(jsonPath("$.components.inventory.details.outOfStock").value(0))
                .andExpect(jsonPath("$.components.inventory.details.stocks['SKU-001']").value(50));
    }

    @Test
    @DisplayName("/actuator/health/liveness 与 /readiness：探针分组可用")
    void probesAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("库存归零 → health 变 503 DOWN；恢复后又回 200")
    void healthGoesDownWhenStockEmpty() throws Exception {
        mockMvc.perform(post("/inventory/SKU-001/stock").param("value", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.inventory.status").value("DOWN"))
                .andExpect(jsonPath("$.components.inventory.details.outOfStock").value(1));

        // 复位，避免影响其它用例
        mockMvc.perform(post("/inventory/SKU-001/stock").param("value", "50"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- info ----------

    @Test
    @DisplayName("/actuator/info：自定义 InfoContributor 的内容合并进去了")
    void infoComesFromContributor() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("12-monitor"))
                .andExpect(jsonPath("$.app.owner").value("zsj"))
                .andExpect(jsonPath("$.app.version").value("0.0.1-SNAPSHOT"))
                .andExpect(jsonPath("$.java").isNotEmpty())
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    // ---------- metrics ----------

    @Test
    @DisplayName("/actuator/metrics：自定义指标出现在列表里")
    void metricsListContainsBusinessMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem(BusinessMetrics.ORDER_PLACED)))
                .andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem(BusinessMetrics.ORDER_AMOUNT)))
                .andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem(BusinessMetrics.ORDER_PROCESS)))
                .andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem(BusinessMetrics.LOW_STOCK)))
                // 框架自带的一起在，说明两套指标没有互相覆盖
                .andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem("jvm.memory.used")));
    }

    @Test
    @DisplayName("/actuator/metrics/{name}：能查到单指标的 measurements 与标签")
    void singleMetricDetail() throws Exception {
        mockMvc.perform(get("/actuator/metrics/{name}", BusinessMetrics.ORDER_PLACED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BusinessMetrics.ORDER_PLACED))
                .andExpect(jsonPath("$.measurements[0].statistic").value("COUNT"))
                // 用 tag 区分成功/失败，所以 availableTags 里应该有 result
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'result')]").exists())
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'application')]").exists());
    }

    @Test
    @DisplayName("下单成功后，Counter 相对加一，金额进了 DistributionSummary")
    void metricsGrowAfterOrder() throws Exception {
        double before = registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "success").counter().count();
        long amountCountBefore = registry.get(BusinessMetrics.ORDER_AMOUNT).summary().count();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-003","quantity":2,"amount":129.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remainingStock").value(8));

        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "success").counter().count()).isEqualTo(before + 1);
        assertThat(registry.get(BusinessMetrics.ORDER_AMOUNT).summary().count())
                .isEqualTo(amountCountBefore + 1);
    }

    @Test
    @DisplayName("下单失败同样计数：result=failed 加一、金额不记、耗时照记")
    void failedOrderStillCounted() throws Exception {
        double failedBefore = registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "failed").counter().count();
        long timerBefore = registry.get(BusinessMetrics.ORDER_PROCESS).timer().count();
        long amountBefore = registry.get(BusinessMetrics.ORDER_AMOUNT).summary().count();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-002","quantity":9999,"amount":50.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20409));

        assertThat(registry.get(BusinessMetrics.ORDER_PLACED)
                .tag("result", "failed").counter().count()).isEqualTo(failedBefore + 1);
        assertThat(registry.get(BusinessMetrics.ORDER_PROCESS).timer().count()).isEqualTo(timerBefore + 1);
        assertThat(registry.get(BusinessMetrics.ORDER_AMOUNT).summary().count())
                .as("失败订单不该进金额分布")
                .isEqualTo(amountBefore);
    }

    // ---------- Prometheus ----------

    @Test
    @DisplayName("/actuator/prometheus：指标名转成下划线，公共标签 application 也在")
    void prometheusExposition() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-001","quantity":1,"amount":66.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                // Counter 在 Prometheus 里带 _total 后缀
                .andExpect(content().string(containsString("biz_order_placed_total")))
                .andExpect(content().string(containsString("result=\"success\"")))
                .andExpect(content().string(containsString("result=\"failed\"")))
                // management.metrics.tags.application 生效
                .andExpect(content().string(containsString("application=\"12-monitor\"")))
                // Timer 因为开了 percentiles-histogram，会有 _bucket
                .andExpect(content().string(containsString("biz_order_process_seconds_bucket")));
    }
}
