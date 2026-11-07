package top.zsj.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.zsj.test.common.BusinessException;
import top.zsj.test.common.ErrorCode;
import top.zsj.test.dto.OrderRequest;
import top.zsj.test.dto.OrderResult;
import top.zsj.test.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第二层：切片测试（{@code @WebMvcTest}）。
 * <p>只装 Web 层——Controller、{@code @RestControllerAdvice}、消息转换器、参数校验器，
 * <b>不装</b> Service、不装数据源。Service 用 {@code @MockitoBean} 顶掉，
 * 于是 HTTP 语义（状态码、JSON 结构、异常翻译）可以单独验，且启动只要一两秒。</p>
 *
 * @author zsj
 */
@WebMvcTest(controllers = OrderController.class)
@DisplayName("OrderController 切片测试：只有 Web 层，Service 是 mock")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // ---------- 正常路径 ----------

    @Test
    @DisplayName("POST /orders/calculate：200 + code=0 + 金额与规则原样返回")
    void calculateOk() throws Exception {
        when(orderService.calculate(any(OrderRequest.class))).thenReturn(new OrderResult(
                "NO-OK",
                new BigDecimal("300.00"),
                new BigDecimal("40"),
                new BigDecimal("13.00"),
                new BigDecimal("247.00"),
                100,
                List.of("满300减40", "金卡95折")));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-OK","amount":300,"goldCard":true,"sku":"SKU-001","quantity":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("成功"))
                // 金额用 JSON 整体比对，避开 Double / BigDecimal 的类型差异
                .andExpect(content().json("""
                        {"data":{"orderNo":"NO-OK","payable":247.00,"reduceAmount":40,
                                 "goldCardDiscount":13.00,"appliedRules":["满300减40","金卡95折"]}}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    @DisplayName("GET /orders/quick-calculate：查询参数能正常绑定")
    void quickCalculateOk() throws Exception {
        when(orderService.calculate(any(OrderRequest.class))).thenReturn(new OrderResult(
                "QUICK", new BigDecimal("100.00"), new BigDecimal("10"),
                new BigDecimal("0"), new BigDecimal("90.00"), 100, List.of("满100减10")));

        mockMvc.perform(get("/orders/quick-calculate")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":0,"data":{"payable":90.00}}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    @DisplayName("GET /orders/hello：切片能起来就够了")
    void hello() throws Exception {
        mockMvc.perform(get("/orders/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("hello, testing"));
    }

    // ---------- 参数校验：三种不同的 400 ----------

    @Test
    @DisplayName("请求体缺 orderNo：400 + 20400 + 字段级提示")
    void missingOrderNo() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":300}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("orderNo")))
                .andExpect(jsonPath("$.message").value(containsString("订单号不能为空")));
    }

    @Test
    @DisplayName("金额缺失：400 + 金额不能为空")
    void missingAmount() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("金额不能为空")));
    }

    @Test
    @DisplayName("金额小于 0.01：400 + 金额不能小于 0.01")
    void amountTooSmall() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-1","amount":0.001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("金额不能小于 0.01")));
    }

    @Test
    @DisplayName("请求体不是合法 JSON：400，而不是 500")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("请求体不是合法的 JSON"));
    }

    @Test
    @DisplayName("查询参数类型不对：amount=abc → 400")
    void amountNotANumber() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate")
                        .param("amount", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("参数 amount 类型不正确")));
    }

    @Test
    @DisplayName("缺少必填查询参数：不带 amount → 400")
    void missingQueryParam() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("缺少必填参数 amount")));
    }

    // ---------- 业务异常 -> HTTP 状态码 ----------

    @Test
    @DisplayName("库存不足：业务码 20409 → HTTP 409")
    void outOfStockBecomes409() throws Exception {
        when(orderService.calculate(any(OrderRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.OUT_OF_STOCK,
                        "库存不足：需要 101 件，仅剩 100 件"));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-OOS","amount":100,"quantity":101}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.OUT_OF_STOCK.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("只给错误码不给文案：message 取 ErrorCode 的默认文案")
    void businessExceptionUsesErrorCodeText() throws Exception {
        when(orderService.calculate(any(OrderRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.OUT_OF_STOCK));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-OOS2","amount":100}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorCode.OUT_OF_STOCK.getMessage()));
    }

    @Test
    @DisplayName("意料之外的异常统一变 500，且不把堆栈吐给调用方")
    void unexpectedErrorBecomes500() throws Exception {
        when(orderService.calculate(any(OrderRequest.class)))
                .thenThrow(new IllegalStateException("数据库连接池已耗尽"));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"NO-500","amount":100}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                // 关键：原始异常信息不能出现在响应里
                .andExpect(jsonPath("$.message").value(not(containsString("连接池"))));
    }
}
