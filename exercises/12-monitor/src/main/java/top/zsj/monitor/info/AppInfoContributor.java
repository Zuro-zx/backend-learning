package top.zsj.monitor.info;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义 info 内容。
 * <p>{@code /actuator/info} 默认是空的，因为 Spring 不知道你想暴露什么。
 * 实现 {@link InfoContributor} 之后，每个 contributor 的 detail 会合并成一份 JSON。</p>
 * <p>注意：这里只放「看一眼就知道是哪个版本、找谁」的信息，
 * 不要放连接串、密钥、内网地址——info 端点在很多团队里是对内网裸奔的。</p>
 *
 * @author zsj
 */
@Component
public class AppInfoContributor implements InfoContributor {

    private final Environment environment;
    private final LocalDateTime startedAt = LocalDateTime.now();

    public AppInfoContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("name", environment.getProperty("spring.application.name", "unknown"));
        app.put("version", environment.getProperty("app.version", "unknown"));
        app.put("owner", environment.getProperty("app.owner", "unknown"));
        app.put("profiles", environment.getActiveProfiles().length == 0
                ? "default" : String.join(",", environment.getActiveProfiles()));

        builder.withDetail("app", app)
                .withDetail("java", System.getProperty("java.version"))
                .withDetail("startedAt", startedAt.toString());
    }
}
