package top.zsj.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第三层：集成测试（{@code @SpringBootTest}）。
 * <p>启动完整上下文，真实的 Controller → Service → InventoryClient 一路串下来，
 * 用来兜住「每个零件单独都对、装到一起就不对」的那类问题——
 * 比如 Bean 装配、JSON 序列化配置、异常处理器是否真的被注册。</p>
 * <p>这一层最慢，所以只挑几条最关键的链路，不重复单元测试已经覆盖的分支。</p>
 *
 * @author zsj
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("集成测试：完整上下文 + 真实调用链")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /orders/calculate 全链路：300 元金卡 → 应付 247，库存取自真实库存实现")
    void calculateThroughWholeStack() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"IT-001","amount":300,"goldCard":true,"sku":"SKU-IT","quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().json("""
                        {"data":{"orderNo":"IT-001","originalAmount":300.00,"reduceAmount":40,
                                 "goldCardDiscount":13.00,"payable":247.00,"availableStock":100,
                                 "appliedRules":["满300减40","金卡95折"]}}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    @DisplayName("库存不足走完整异常链：Service 抛业务异常 → 统一响应 → HTTP 409")
    void outOfStockThroughWholeStack() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"IT-002","amount":100,"sku":"SKU-IT","quantity":101}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(20409))
                .andExpect(jsonPath("$.message").value("库存不足：需要 101 件，仅剩 100 件"));
    }

    @Test
    @DisplayName("GET /orders/quick-calculate 全链路：100 元 → 应付 90")
    void quickCalculateThroughWholeStack() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate").param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":0,"data":{"orderNo":"QUICK","payable":90.00}}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    @DisplayName("GET /orders/hello：上下文确实起来了")
    void helloThroughWholeStack() throws Exception {
        mockMvc.perform(get("/orders/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("hello, testing"));
    }
}
