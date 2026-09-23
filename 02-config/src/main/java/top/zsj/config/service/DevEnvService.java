package top.mqxu.config.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 开发环境实现，仅在 dev 环境生效
 *
 * <p>{@code @Service} 是语义化的注解，用它标注的类一样可以被
 * Spring 容器托管为唯一的 Bean；再配合 {@code @Profile("dev")}，
 * 只有激活 dev 环境时这个 Bean 才会被注册进容器。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description dev 环境专属实现
 */
@Service
@Profile("dev")
public class DevEnvService implements EnvService {

    @Override
    public String envInfo() {
        return "我是 dev 环境专属的 Bean";
    }
}
