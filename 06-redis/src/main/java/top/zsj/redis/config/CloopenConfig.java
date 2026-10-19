package top.zsj.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 容联云短信配置绑定
 * <p>
 * 对应 application.yml 的 zsj.sms.ccpp 节点，
 * 凭证在容联云控制台（https://www.yuntongxun.com）HOME 页 / 应用管理里查看。
 *
 * @author zsj
 * @date 2026/10/14
 * @description 容联云短信配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "zsj.sms.ccpp")
public class CloopenConfig {

    /** 容联云 REST 服务器地址（固定 app.cloopen.com） */
    private String serverIp;

    /** 容联云 REST 服务器端口（固定 8883） */
    private Integer port;

    /** 主账户 ACCOUNT SID（控制台 HOME 页，8 位） */
    private String accountSid;

    /** 主账户令牌 ACCOUNT TOKEN（控制台 HOME 页，点查看可见，32 位） */
    private String accountToken;

    /** 应用 APP ID（管理 → 应用管理，默认应用的 APP ID，32 位） */
    private String appId;

    /** 短信模板 ID（测试模板固定为 1） */
    private String templateId;
}
