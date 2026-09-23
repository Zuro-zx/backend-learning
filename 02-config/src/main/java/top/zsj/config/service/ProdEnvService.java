package top.mqxu.config.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 生产环境实现，仅在 prod 环境生效
 *
 * <p>代码与 {@link DevEnvService} 完全一致，只是
 * {@code @Profile("prod")} 表示只有激活 prod 环境时
 * 这个 Bean 才会被注册进容器。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description prod 环境专属实现
 */
@Service
@Profile("prod")
public class ProdEnvService implements EnvService {

    @Override
    public String envInfo() {
        return "我是 prod 环境专属的 Bean";
    }
}
