package top.zsj.test.client;

import org.springframework.stereotype.Component;

/**
 * 库存客户端的本地实现：固定返回 100 件，不依赖任何外部系统。
 * <p>真实项目里这里会是 RestClient / Feign 调用，测试里用 {@code @MockitoBean} 换掉它。</p>
 *
 * @author zsj
 */
@Component
public class DefaultInventoryClient implements InventoryClient {

    private static final int DEFAULT_STOCK = 100;

    @Override
    public int stockOf(String sku) {
        return DEFAULT_STOCK;
    }
}
