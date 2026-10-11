package top.zsj.mybatis.service;

import com.baomidou.mybatisplus.spring.service.IService;
import top.zsj.mybatis.entity.Order;

import java.util.List;

/**
 * 订单业务接口
 *
 * @author zsj
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单并扣减库存，两个写操作在同一个事务里，要么都成功要么都回滚
     *
     * @param simulateError true 时在两步都完成后抛异常，用来验证原子性
     */
    Order createOrder(Long userId, Long productId, Integer quantity, boolean simulateError);

    List<Order> listByUserId(Long userId);
}
