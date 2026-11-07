# 12-monitor · 应用监控综合练习

课程模块 12《应用监控 —— 出问题之前就看见》的练习实现。三个问题的分工先说清楚：

| 问题 | 答案由谁给 | 本模块的实现 |
| --- | --- | --- |
| 这个实例还能不能接流量？ | Health + 探针 | `InventoryHealthIndicator` |
| 现在业务跑得怎么样？ | Metrics（Counter / Gauge / Timer / Summary） | `BusinessMetrics` |
| 这个实例跑的是哪个版本、找谁？ | Info | `AppInfoContributor` |

## 三件套与依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

第一个给端点，第二个给 Prometheus 抓取格式。**只引第一个的话 `/actuator/prometheus` 是 404**——Actuator 自己不产生 Prometheus 文本，它只负责把 `MeterRegistry` 里的指标暴露出去，格式由 registry 决定。

## 四种仪表，各用一次

| 类型 | 指标名 | 为什么是它 | Prometheus 里的样子 |
| --- | --- | --- | --- |
| Counter | `biz.order.placed` | 只增不减的累计数，用 `result` 标签区分成功/失败 | `biz_order_placed_total{result="success"}` |
| Gauge | `biz.inventory.low.stock` | 可增可减的瞬时值，注册的是「对象 + 取值函数」 | `biz_inventory_low_stock 0.0` |
| Timer | `biz.order.process` | 既计时长又计次数，还能算分位 | `biz_order_process_seconds_count` / `_bucket` |
| DistributionSummary | `biz.order.amount` | 不是时间的「分布」，比如金额 | `biz_order_amount_CNY_count` / `_sum` |

四条都统一加 `biz.` 前缀，和框架自带的 `http.server.requests`、`jvm.memory.used` 分开，看板上不会混。

## 端点清单（`management.endpoints.web.exposure.include`）

| 端点 | 用途 |
| --- | --- |
| `/actuator/health` | 整体健康，`show-details: always` 时带每个组件的 details |
| `/actuator/health/liveness` | 存活探针：进程卡死了吗？失败应该重启 |
| `/actuator/health/readiness` | 就绪探针：能接流量了吗？失败应该摘掉，但别重启 |
| `/actuator/info` | 版本、负责人、启动时间 |
| `/actuator/metrics` | 指标名列表 |
| `/actuator/metrics/{name}` | 单个指标的 measurements 与标签 |
| `/actuator/prometheus` | Prometheus 抓取地址 |
| `/actuator/loggers` | 运行时查 / 改日志级别，排查线上问题最常用的一个 |

> `liveness` / `readiness` 两个分组**默认不存在**，必须开 `management.endpoint.health.probes.enabled: true`（非 Kubernetes 环境下不会自动创建）。

## 目录结构

```
12-monitor
├── pom.xml
├── verify.sh                        # 18 组实测脚本（curl）
├── src/main/java/top/zsj/monitor
│   ├── MonitorApplication.java
│   ├── common        Result、ErrorCode、BusinessException、GlobalExceptionHandler
│   ├── controller    OrderController、InventoryController
│   ├── dto           OrderRequest、OrderResult
│   ├── health        InventoryHealthIndicator     ← 自定义健康检查
│   ├── info          AppInfoContributor           ← 自定义 info
│   ├── inventory     InventoryStore               ← 被三处消费的业务状态
│   ├── metrics       BusinessMetrics              ← 四种仪表
│   └── service       OrderService                 ← 埋点位置
└── src/test/java/top/zsj/monitor
    ├── metrics/BusinessMetricsTest          7 个用例（SimpleMeterRegistry）
    ├── health/InventoryHealthIndicatorTest  3 个用例
    └── MonitorEndpointTest                  9 个用例（@SpringBootTest 端点全链路）
```

## 几个值得单独说的设计判断

### 埋点写在 Service，不写在 Controller

Controller 只管协议，业务成功与否由 Service 判定，指标应该跟着业务判定走。

耗时统计用 `try/finally` 而不是只记成功分支：

```java
long start = System.nanoTime();
boolean success = false;
try {
    if (!inventoryStore.deduct(request.sku(), request.quantity())) {
        throw new BusinessException(ErrorCode.OUT_OF_STOCK, "...");
    }
    success = true;
    return new OrderResult(...);
} finally {
    metrics.recordOrder(request.amount(), System.nanoTime() - start, success);
}
```

**失败的耗时同样是信号**。只记成功的话，一次「大量请求全在 3 秒后失败」的故障在看板上会彻底消失——那正是最需要看到的时候。

### 失败的订单不记金额

`DistributionSummary` 只收成功订单的金额。否则一份「金额分布」里混进了失败订单的意图金额，P90 会被带偏，容量规划跟着错。

### 健康检查判定「归零」，不判定「低库存」

```java
Health.Builder builder = outOfStock > 0 ? Health.down() : Health.up();
```

为什么不是「低于预警线就 DOWN」？因为健康检查的语义是「**还能不能对外提供服务**」。低于预警线仍然能卖，只是该补货了——那属于告警（Gauge + 告警规则），不该把实例从负载均衡里摘掉。把健康检查和业务告警混在一起，是线上最常见的误配之一。

### Gauge 注册的是引用，不是快照

```java
Gauge.builder(LOW_STOCK, inventoryStore, store -> (double) store.lowStockCount())
```

注册时**不会**调用 `lowStockCount()`，是每次抓取才读一次，所以指标永远是最新值。如果写成 `Gauge.builder(name, store.lowStockCount())`（传值），指标会永久定格在注册那一瞬间的数字——这个错非常常见，而且看起来「有数据」，很难发现。

## 实测结果（18 组，`bash verify.sh 8012`）

| # | 场景 | 期望 | 实际 |
| --- | --- | --- | --- |
| 1 | `/actuator/health` | 200 `status=UP` | ✅ |
| 2 | health 里含自定义组件 | `components.inventory.details.skuCount=3` | ✅ |
| 3 | `/actuator/health/liveness` | 200 UP | ✅ |
| 4 | `/actuator/health/readiness` | 200 UP | ✅ |
| 5 | `/actuator/info` | `app.owner=zsj` | ✅ |
| 6 | 下单 | 200，`remainingStock` 递减 | ✅ |
| 7 | `/actuator/metrics` | 列表含 `biz.order.placed` | ✅ |
| 8 | 单指标详情 | `availableTags` 含 `result` | ✅ |
| 9 | 金额指标 | `biz.order.amount` 存在 | ✅ |
| 10 | 耗时指标 | `biz.order.process` 存在 | ✅ |
| 11 | Prometheus 计数器 | `biz_order_placed_total` | ✅ |
| 12 | Prometheus 公共标签 | `application="12-monitor"` | ✅ |
| 13 | Prometheus 直方图 | `biz_order_process_seconds_bucket` | ✅ |
| 14 | 库存设为 3 | Gauge 变 `1.0` | ✅ |
| 15 | 低库存但没归零 | health 仍 200 UP | ✅ |
| 16 | 库存设为 0 | health 变 **503** DOWN | ✅ |
| 17 | 归零后下单 | 409 `code=20409` | ✅ |
| 18 | 库存恢复 | health 回 200 | ✅ |

`mvn test` 实测：`Tests run: 19, Failures: 0, Errors: 0`。

Prometheus 实际输出（自定义指标部分，已去掉 `_bucket` 噪声）：

```
biz_inventory_low_stock{application="12-monitor"} 0.0
biz_order_amount_CNY_count{application="12-monitor"} 1
biz_order_amount_CNY_sum{application="12-monitor"} 129.0
biz_order_amount_CNY_max{application="12-monitor"} 129.0
biz_order_placed_total{application="12-monitor",result="failed"} 1.0
biz_order_placed_total{application="12-monitor",result="success"} 1.0
biz_order_process_seconds_count{application="12-monitor"} 2
biz_order_process_seconds_sum{application="12-monitor"} 0.0014108
biz_order_process_seconds_max{application="12-monitor"} 8.963E-4
```

`/actuator/prometheus` 全量输出 317 行。

## 踩坑记录

1. **Boot 4 把健康检查从 `actuate` 拆出去了**
   `Health` / `HealthIndicator` / `Status` 在 Boot 4 里不在 `org.springframework.boot.actuate.health` 了，而是独立模块 `spring-boot-health`，包路径 `org.springframework.boot.health.contributor`。照旧教程 import 会直接报「找不到符号」。`Info` / `InfoContributor` 还在 `org.springframework.boot.actuate.info`，没动。

2. **指标名别用 Prometheus 保留后缀**
   第一版计数器叫 `biz.order.created`，`/actuator/metrics` 里一切正常，但 `/actuator/prometheus` 里变成了 **`biz_order_total`**——Micrometer 把 `created` 当成保留后缀剥掉了，「下单」这个语义凭空消失，还容易和别的 `biz_order_*` 撞名。
   `total / count / sum / max / min / bucket / created` 都别放在指标名最后一段。改成 `biz.order.placed` 后是干净的 `biz_order_placed_total`。
   **教训：验指标一定要看 Prometheus 输出，不能只看 `/actuator/metrics`。**

3. **`publishPercentiles` 会额外注册 meter**
   `DistributionSummary.builder().publishPercentiles(0.5, 0.9)` 除了 summary 本身，还会为每个分位注册一个 Gauge。所以「注册表里有几个 meter」不能写死成 5。测试改成断言「记录之后数量没变」，这才是真正想验的（注册幂等）。

4. **断言 `count()` 不要混用 double**
   `Counter.count()` 返回 `double`，`DistributionSummary.count()` / `Timer.count()` 返回 `long`。写 `assertThat(summary.count()).isEqualTo(before + 1)` 时，若 `before` 是 `double`，实际比较的是 `Double(2.0)` 和 `Long(2L)`，值相等但类型不等，断言会失败——报错信息还是极具误导性的 `expected: 2.0 but was: 2L`。变量声明成 `long` 就好。

5. **`/actuator/health` DOWN 时返回 503，不是 200**
   如果这个服务前面挂着负载均衡，503 会被自动摘节点，这正是想要的行为。但如果你在测试里断言 `status().isOk()`，会莫名其妙红掉。

6. **`env` / `beans` / `loggers` 不要无脑 `include: "*"`**
   `env` 会把配置项全吐出来（包括数据源 URL，甚至没加掩码的凭证），`beans` 会暴露内部结构。本模块只开了 `health,info,metrics,prometheus,loggers`，生产上还要配合 Spring Security 加鉴权（模块 8 的六条链可以直接复用）给 `/actuator/**` 加一层。

## 附：本机运行

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"
export PATH="E:/tools/apache-maven-3.9.16/bin:$PATH"

mvn test
mvn -DskipTests package
java -jar target/12-monitor-0.0.1-SNAPSHOT.jar    # 端口 8012
bash verify.sh 8012
```

```bash
# 手验
curl -s localhost:8012/actuator/health | jq
curl -s -X POST localhost:8012/orders -H 'Content-Type: application/json' \
     -d '{"sku":"SKU-001","quantity":3,"amount":129.00}'
curl -s localhost:8012/actuator/metrics/biz.order.placed | jq
curl -s localhost:8012/actuator/prometheus | grep '^biz_'
```

> 起服务前记得 `unset SERVER__PORT`，否则 Spring 的 `server.port` 会被环境变量覆盖。
