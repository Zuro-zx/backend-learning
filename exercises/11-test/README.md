# 11-test · 测试综合练习

课程模块 11《测试 —— 让改动有底气》的练习实现。核心不是"写几个用例"，而是把**三层粒度**各自的适用边界划清楚：

| 层 | 注解 | 装了什么 | 没装什么 | 能测出什么 | 本模块用例 |
| --- | --- | --- | --- | --- | --- |
| ① 单元 | `@ExtendWith(MockitoExtension.class)` | 一个类 + `@Mock` | Spring、数据库、HTTP | 业务规则、边界值、分支 | 16 |
| ② 切片 | `@WebMvcTest` + `@MockitoBean` | Web 层（Controller、`@RestControllerAdvice`、转换器、校验器） | Service、数据源 | 状态码、JSON 结构、异常翻译 | 12 |
| ③ 集成 | `@SpringBootTest` + `@AutoConfigureMockMvc` | 全部 | — | Bean 装配、序列化配置、端到端链路 | 4 |

三层加起来 32 个用例，`mvn test` 约 12 秒跑完。

> **为什么不让所有用例都写集成测试？** 因为慢。本模块集成层只写 4 个用例，专门覆盖"每个零件单独都对、装到一起就不对"的那类问题；业务分支的穷举交给单元测试，HTTP 语义的穷举交给切片测试。

## 被测业务

下单试算，规则一共四条（教程第 4 节）：

| 规则 | 实现 |
| --- | --- |
| 满 300 减 40 | `original >= 300` |
| 满 100 减 10 | `100 <= original < 300`，两档互斥 |
| 金卡 95 折 | **满减之后**再乘 0.95，不是对原价打折 |
| 应付不低于 0.01 | `payable.compareTo(0.01) < 0` 时兜到 0.01 |

`OrderService.calculate` 里一共 5 个分支点：库存够不够、是否满 300、是否满 100、是否金卡、是否触底。JaCoCo 对 `OrderService` 的分支覆盖是 **12/12**。

## 目录结构

```
11-test
├── pom.xml                        # 含 jacoco 0.8.16，report 绑在 test 阶段
├── verify.sh                      # 12 组实测脚本（curl）
├── src/main/java/top/zsj/test
│   ├── TestApplication.java
│   ├── client        InventoryClient（接口）、DefaultInventoryClient（固定返回 100）
│   ├── common        Result、ErrorCode、BusinessException、GlobalExceptionHandler
│   ├── controller    OrderController
│   ├── dto           OrderRequest（record）、OrderResult（record）
│   └── service       OrderService
└── src/test/java/top/zsj/test
    ├── service/OrderServiceTest            16 个用例（单元）
    ├── controller/OrderControllerTest      12 个用例（切片）
    └── OrderIntegrationTest                 4 个用例（集成）
```

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/orders/calculate` | 完整试算，请求体走 `@Valid` |
| GET | `/orders/quick-calculate?amount=&goldCard=` | 快速试算，只传金额和卡种 |
| GET | `/orders/hello` | 冒烟接口 |

```bash
curl -s -X POST http://127.0.0.1:8011/orders/calculate \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"REAL-001","amount":300,"goldCard":true,"sku":"SKU-1","quantity":2}'
```

```json
{"code":0,"message":"成功","data":{"orderNo":"REAL-001","originalAmount":300.00,"reduceAmount":40,
 "goldCardDiscount":13.00,"payable":247.00,"availableStock":100,
 "appliedRules":["满300减40","金卡95折"]}}
```

`OrderResult` 把优惠拆成 `reduceAmount` / `goldCardDiscount` / `payable` 三个字段，而不是只给一个最终价——断言可以精确到"哪一步省了多少钱"，出问题时一眼能看出是满减算错了还是折扣算错了。

## JaCoCo 覆盖率（实测）

`mvn test` 之后看 `target/site/jacoco/index.html`。

| 汇总 | 覆盖 / 总数 | 比例 |
| --- | --- | --- |
| 指令 | 497 / 507 | 98.0% |
| 分支 | 22 / 24 | 91.7% |
| 行 | 99 / 101 | 98.0% |

按类拆开：

| 类 | 行覆盖 | 分支覆盖 |
| --- | --- | --- |
| `OrderService` | 40/40 | 12/12 |
| `OrderController` | 6/6 | — |
| `OrderRequest` | 6/6 | 8/8 |
| `OrderResult` | 1/1 | — |
| `Result` | 4/4 | — |
| `ErrorCode` | 13/13 | — |
| `BusinessException` | 6/6 | — |
| `DefaultInventoryClient` | 2/2 | — |
| `GlobalExceptionHandler` | 20/20 | 2/4 |
| `TestApplication` | 1/3 | — |

**剩下 3 处没覆盖，都是故意的**：

1. `TestApplication.main`（2 行）—— 启动入口，`@SpringBootTest` 走的是测试启动器，不会执行它。
2. `GlobalExceptionHandler` 里两个防御性三元表达式：`detail.isEmpty() ? 默认文案 : detail` 和 `required == null ? "未知类型" : ...`。前者只有在 `MethodArgumentNotValidException` 一个字段错误都没有时才会命中，后者只有在类型装换失败却拿不到目标类型时才命中——两者在框架层面都构造不出来，属于纯保险代码。

> 覆盖率不是 KPI。第一版跑完是 93.7%，JaCoCo 的红色行提示我 `handleUnexpected`（兜底 500）、`Result.fail(ErrorCode)` 单参重载、`BusinessException` 单参构造器三处**完全没被测到**——补两个用例就把它们拉到 98%。这才是覆盖率报告的正确用法：**找漏，不是刷分**。

## 三层测试各自解决什么

### 单元测试：把外部依赖换成 mock

`InventoryClient` 抽成接口，就是为了在单元测试里能随手写出"库存只剩 0 件"：

```java
@Mock
private InventoryClient inventoryClient;

@InjectMocks
private OrderService orderService;

@BeforeEach
void stubInventory() {
    when(inventoryClient.stockOf(anyString())).thenReturn(100);
}
```

`MockitoExtension` 默认 **strict stubs**：stub 了但没被调用会直接让用例失败。所以这里的 stub 放在 `@BeforeEach`，并保证每个用例都会执行到 `calculate()`——不这么写就会被 `UnnecessaryStubbingException` 拦下来。想测库存不足，不用重新 stub，只要把 `quantity` 调到 101。

### 切片测试：只装 Web 层

```java
@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private OrderService orderService;   // Spring 6.2+ 取代 @MockBean
}
```

`@RestControllerAdvice` 属于 Web 层的组件过滤范围，所以**不用**额外 `@Import`，异常翻译就能被一起测到。这一个类里覆盖了四种不同的 400 + 一种 409：

| 场景 | 异常 | 状态码 |
| --- | --- | --- |
| `@NotBlank` / `@NotNull` / `@DecimalMin` 不通过 | `MethodArgumentNotValidException` | 400 |
| `?amount=abc` | `MethodArgumentTypeMismatchException` | 400 |
| 少了 `amount` | `MissingServletRequestParameterException` | 400 |
| 请求体不是 JSON | `HttpMessageNotReadableException` | 400 |
| 库存不足 | 业务异常 `OUT_OF_STOCK` | 409 |

### 集成测试：只挑关键链路

4 个用例：完整试算链路、库存不足的完整异常链、GET 试算链路、冒烟。真实 `DefaultInventoryClient` 返回 100，所以断言里能直接看到 `"availableStock":100`——说明这条路真的走完了 Service 和 Client，而不是被 mock 短路。

## 实测结果（12 组，`bash verify.sh 8011`）

| # | 场景 | 期望 | 实际 |
| --- | --- | --- | --- |
| 1 | `GET /orders/hello` | 200 `hello, testing` | ✅ |
| 2 | 300 元金卡 | 200 `payable=247.00` | ✅ |
| 3 | 100 元普卡 | 200 `payable=90.00` | ✅ |
| 4 | quick 100 金卡 | 200 `payable=85.50` | ✅ |
| 5 | 0.01 元金卡 | 200 命中 `触底0.01` | ✅ |
| 6 | `quantity=101` 库存不足 | 409 `code=20409` | ✅ |
| 7 | 缺 `amount` | 400 `amount 金额不能为空` | ✅ |
| 8 | `amount=0.001` | 400 `金额不能小于 0.01` | ✅ |
| 9 | `orderNo="  "` | 400 `订单号不能为空` | ✅ |
| 10 | 非法 JSON | 400 `请求体不是合法的 JSON` | ✅ |
| 11 | `?amount=abc` | 400 `参数 amount 类型不正确，期望 BigDecimal` | ✅ |
| 12 | quick 不带 `amount` | 400 `缺少必填参数 amount` | ✅ |

`mvn test` 实测：`Tests run: 32, Failures: 0, Errors: 0`。

## 踩坑记录

1. **Boot 4 的测试注解换了包**
   `@WebMvcTest`、`@AutoConfigureMockMvc` 在 Boot 4 里从 `org.springframework.boot.test.autoconfigure.web.servlet` 挪到了 `org.springframework.boot.webmvc.test.autoconfigure`，而且要额外引 `spring-boot-starter-webmvc-test`（`spring-boot-starter-test` 里没有）。只加后者不加依赖会报"找不到符号"。

2. **Spring 7 不再用 skyscreamer 的 `JSONCompareMode`**
   `content().json(String, JSONCompareMode)` 这个重载的第一个参数现在是 `org.springframework.test.json.JsonCompareMode`，只有 `STRICT` / `LENIENT` 两个值。照着旧教程写 `org.skyscreamer.jsonassert.JSONCompareMode.LENIENT` 会编译失败（spring-test 7 自带了 JSON 比较实现）。

3. **`@MockBean` 已废弃，要用 `@MockitoBean`**
   包路径 `org.springframework.test.context.bean.override.mockito.MockitoBean`。

4. **record 的紧凑构造器跑在参数校验之前**
   一开始给 `goldCard` 加了 `@NotNull` 又在紧凑构造器里补 `false`，结果 `@NotNull` 永远不触发——Spring 拿到的是已经补好的值。结论：**要补默认值的字段就不要加"非空"校验**，两者只能留一个。

5. **"触底 0.01"这个分支差点变成死代码**
   0.01 × 0.95 = 0.0095。如果先 `setScale(2, HALF_UP)` 再判断，0.0095 已经被舍成 0.01，`payable < 0.01` 永远为假，分支覆盖只能到 11/12。正确顺序是**先比较、后取舍**。这个 bug 单独看代码几乎发现不了，是 JaCoCo 报出"分支没走满"才揪出来的。

6. **金额断言不要用 `jsonPath().value(Double)`**
   `BigDecimal` 序列化成 `247.00`，jsonPath 解析出来的类型可能是 `Double` 也可能是 `BigDecimal`，`assertEquals` 会因为类型不同而误报。改法有两种：用 `content().json(..., LENIENT)` 整体比对（本模块采用），或者断言字符串。

7. **`@ExceptionHandler(Exception.class)` 是必须的**
   没有它，`IllegalStateException` 会变成 500 但响应体是 Spring 默认的错误 JSON（带 `trace` 字段），既不符合统一响应格式，还可能把堆栈泄给调用方。补上兜底处理器后，用例里能直接断言"响应里不含原始异常信息"。

## 附：本机运行

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"   # 本机 JAVA_HOME 多写了一层 /21
export PATH="E:/tools/apache-maven-3.9.16/bin:$PATH"

mvn test                       # 32 个用例 + 生成 target/site/jacoco/index.html
mvn -DskipTests package
java -jar target/11-test-0.0.1-SNAPSHOT.jar     # 端口 8011
bash verify.sh 8011            # 12 组 curl 实测
```

> 用 WorkBuddy 起服务前记得 `unset SERVER__PORT`，否则 Spring 的 `server.port` 会被环境变量覆盖。
