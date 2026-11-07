package top.zsj.monitor.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.monitor.common.Result;
import top.zsj.monitor.dto.OrderRequest;
import top.zsj.monitor.dto.OrderResult;
import top.zsj.monitor.service.OrderService;

/**
 * 下单接口：这是被监控的业务入口。
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

    @PostMapping
    public Result<OrderResult> place(@Valid @RequestBody OrderRequest request) {
        return Result.ok(orderService.place(request));
    }
}
