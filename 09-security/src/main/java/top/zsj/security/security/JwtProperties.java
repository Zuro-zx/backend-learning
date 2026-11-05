package top.zsj.security.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模块自有配置：JWT 与 CORS
 * <p>
 * 对应 application.yml 里的 security.jwt 与 security.cors 前缀。
 * 密钥长度必须 >= 32 字节（HS256 要求），生产环境改从环境变量注入。
 *
 * @author zsj
 */
@Data
@ConfigurationProperties(prefix = "security")
public class JwtProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        /** HS256 签名密钥 */
        private String secret;
        /** 令牌有效期，分钟（BR-6：120 分钟，不做刷新） */
        private long expireMinutes = 120;
    }

    @Data
    public static class Cors {
        /** 分离部署时允许的前端来源 */
        private String allowedOrigins = "http://localhost:5173";
    }
}
