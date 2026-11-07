package top.zsj.test.client;

/**
 * 库存服务客户端。
 * <p>故意抽成接口：单元测试只要 mock 这个接口，就能在不启任何容器的情况下
 * 把「外部依赖」这一层彻底隔离掉。</p>
 *
 * @author zsj
 */
public interface InventoryClient {

    /**
     * 查询某个 SKU 的可售库存。
     *
     * @param sku 商品编码
     * @return 可售数量
     */
    int stockOf(String sku);
}
