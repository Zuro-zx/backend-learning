package top.zsj.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 安全相关配置
 * <p>
 * 教程原文前缀是 {@code mqxu.security}，这里按本仓库约定统一改成 {@code zsj.security}。
 *
 * @author zsj
 */
@Data
@ConfigurationProperties(prefix = "zsj.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();

    private Sms sms = new Sms();

    private Cors cors = new Cors();

    /** JWT 签发配置 */
    @Data
    public static class Jwt {

        /** 密钥：64 字节选 HS512，48 字节 HS384，32 字节及以下 HS256 */
        private String secret;

        /** 有效期（分钟） */
        private long expireMinutes = 120;

        /** 存储实现：redis / memory */
        private String store = "redis";
    }

    /** 短信验证码配置 */
    @Data
    public static class Sms {

        /** 存储实现：redis / memory，由 @ConditionalOnProperty 二选一装配 */
        private String store = "redis";

        /** 演示开关：true 时把验证码写进响应，本地 curl 不用翻日志；生产必须 false */
        private boolean exposeCode = false;

        /** 验证码有效期 */
        private Duration codeTtl = Duration.ofMinutes(5);

        /** 同一手机号两次发送的最小间隔 */
        private Duration sendInterval = Duration.ofSeconds(60);

        /** 同一验证码最多允许输错几次 */
        private int maxFailCount = 5;

        /** 未注册手机号首次登录是否自动建号 */
        private boolean autoRegister = false;

        /** 图形验证码有效期（练习 5） */
        private Duration captchaTtl = Duration.ofMinutes(2);
    }

    /** 跨域配置 */
    @Data
    public static class Cors {

        private List<String> allowedOrigins = List.of();
    }
}
