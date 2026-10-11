package top.zsj.config.service;

/**
 * 环境信息服务，用于演示 {@code @Profile} 注解
 *
 * <p>同一个接口，不同环境下注入不同的实现：
 * 开发环境注入 {@link DevEnvService}，生产环境注入 {@link ProdEnvService}。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description 环境服务接口
 */
public interface EnvService {

    /**
     * 返回当前环境的信息描述
     *
     * @return 环境信息
     */
    String envInfo();
}
