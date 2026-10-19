package top.zsj.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码策略配置
 * <p>
 * 对应 application.yml 的 zsj.sms 节点（不含 ccpp 凭证子节点，绑定器会忽略不认识的子键）。
 * 这些数字前端也要用（倒计时、有效期提示），改动时两边一起改。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 验证码策略配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "zsj.sms")
public class SmsProperties {

    /** 是否使用模拟通道：true 时不调用短信 SDK，验证码只写日志与 Redis，方便本地调试 */
    private boolean mockEnabled = true;

    /** 验证码有效期（秒） */
    private long codeExpireSeconds = 300;

    /** 同一手机号两次发送的最小间隔（秒），前端倒计时用同一个数字 */
    private long sendIntervalSeconds = 60;

    /** 同一手机号一天最多发送条数 */
    private long sendDailyLimit = 10;

    /** 同一个验证码最多允许输错的次数，超过就作废 */
    private int maxFailCount = 5;
}
