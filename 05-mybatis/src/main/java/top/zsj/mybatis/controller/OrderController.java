package top.zsj.mybatis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.mybatis.entity.Order;
import top.zsj.mybatis.service.OrderService;

import java.util.List;

/**
 * 订单接口，路径前缀 /orders
 *
 * @author zsj
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /* 增：创建订单 + 扣库存，同一事务 */
    @PostMapping
    public Order create(@RequestParam(required = false) Long userId,
                        @RequestParam Long productId,
                        @RequestParam Integer quantity,
                        @RequestParam(defaultValue = "false") boolean simulateError) {
        return orderService.createOrder(userId, productId, quantity, simulateError);
    }

    /* 查 */
    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping
    public List<Order> list(@RequestParam(required = false) Long userId) {
        return orderService.listByUserId(userId);
    }

    /* 删：逻辑删除 */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return orderService.removeById(id);
    }
}
