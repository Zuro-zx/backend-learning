package top.zsj.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zsj.mybatis.entity.Order;
import top.zsj.mybatis.entity.Product;
import top.zsj.mybatis.mapper.OrderMapper;
import top.zsj.mybatis.mapper.ProductMapper;
import top.zsj.mybatis.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单业务实现：演示「创建订单 + 扣库存」的事务原子性
 *
 * @author zsj
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductMapper productMapper;

    public OrderServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Order createOrder(Long userId, Long productId, Integer quantity, boolean simulateError) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("购买数量必须大于 0");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在：" + productId);
        }

        // 第一步：插入订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        save(order);

        // 第二步：扣库存。setSql 直接用数据库表达式避免并发下读改写丢失更新，
        // 带 stock >= quantity 条件， affected rows = 0 说明库存不足，抛异常连同订单一起回滚
        boolean deducted = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity)) > 0;
        if (!deducted) {
            throw new IllegalStateException("库存不足：商品 " + product.getName() + " 剩余 " + product.getStock());
        }

        // 第三步：模拟业务异常，观察前两步是否都被回滚
        if (simulateError) {
            throw new RuntimeException("模拟业务异常，验证事务回滚");
        }
        return order;
    }

    @Override
    public List<Order> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<Order>()
                .eq(userId != null, Order::getUserId, userId)
                .orderByDesc(Order::getId));
    }

    /** 订单号：时间戳 + 6 位随机数，够演示用 */
    private String generateOrderNo() {
        return ORDER_NO_FORMAT.format(LocalDateTime.now())
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
