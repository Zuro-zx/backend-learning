package top.zsj.test.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.test.common.Result;
import top.zsj.test.dto.OrderRequest;
import top.zsj.test.dto.OrderResult;
import top.zsj.test.service.OrderService;

import java.math.BigDecimal;

/**
 * 试算接口。
 * <p>三个接口是有意设计的：POST 走请求体校验、GET 走查询参数（能顺带测出类型不匹配和参数缺失）、
 * hello 用来验证切片能正常起来。</p>
 *
 * @author zsj
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 完整试算：请求体校验 + 业务计算。 */
    @PostMapping("/calculate")
    public Result<OrderResult> calculate(@Valid @RequestBody OrderRequest request) {
        return Result.ok(orderService.calculate(request));
    }

    /** 快速试算：只传金额和卡种，其余走默认值。 */
    @GetMapping("/quick-calculate")
    public Result<OrderResult> quickCalculate(@RequestParam BigDecimal amount,
                                              @RequestParam(required = false) Boolean goldCard) {
        return Result.ok(orderService.calculate(OrderRequest.of(amount, goldCard)));
    }

    /** 冒烟接口：切片测试里最先跑通的那个。 */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok("hello, testing");
    }
}
