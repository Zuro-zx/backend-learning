package top.mqxu.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 应用配置属性类
 *
 * <p>用法：{@code @ConfigurationProperties} + {@code @Validated} 配置校验。
 * 配置值在应用绑定时校验，不合法会直接报错阻止启动，
 * 避免把错误配置带到运行期。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description app 前缀配置的绑定与校验对象
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "应用名称 app.name 不能为空")
    private String name;

    /** 占位符引用，将 app.author = ${mqxu.name} */
    @NotBlank(message = "应用作者 app.author 不能为空")
    private String author;

    @Min(value = 1, message = "app.port 必须大于等于 1")
    @Max(value = 65535, message = "app.port 必须小于等于 65535")
    private Integer port;

    /** 范围限定：max-count -> maxCount */
    @Min(value = 1, message = "app.max-count 必须大于等于 1")
    @Max(value = 1000, message = "app.max-count 不能超过 1000")
    private Integer maxCount;
}
