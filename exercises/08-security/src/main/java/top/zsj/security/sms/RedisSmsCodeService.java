package top.zsj.security.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.config.SecurityProperties;

import java.time.Duration;

/**
 * Redis 版验证码服务
 * <p>
 * 三个 key：{@code sms:code:{phone}} 存验证码、{@code sms:limit:{phone}} 做发送冷却、
 * {@code sms:fail:{phone}} 记错误次数。验证码一次性，校验通过立刻删。
 *
 * @author zsj
 */
@Service
@ConditionalOnProperty(prefix = "zsj.security.sms", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisSmsCodeService implements SmsCodeService {

    private static final String CODE_PREFIX = "sms:code:";

    private static final String LIMIT_PREFIX = "sms:limit:";

    private static final String FAIL_PREFIX = "sms:fail:";

    private final StringRedisTemplate redis;

    private final SecurityProperties properties;

    public RedisSmsCodeService(StringRedisTemplate redis, SecurityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public String sendCode(String phone) {
        SecurityProperties.Sms sms = properties.getSms();
        String limitKey = LIMIT_PREFIX + phone;
        if (Boolean.TRUE.equals(redis.hasKey(limitKey))) {
            throw new BusinessException(ErrorCode.SMS_SEND_TOO_OFTEN);
        }

        String code = SmsCodes.random6Digit();
        Duration ttl = sms.getCodeTtl();
        // 有效期与冷却分开算：有效期 5 分钟，冷却 60 秒，两者互不影响
        redis.opsForValue().set(CODE_PREFIX + phone, code, ttl);
        redis.opsForValue().set(limitKey, "1", sms.getSendInterval());
        redis.delete(FAIL_PREFIX + phone);
        return code;
    }

    @Override
    public void verify(String phone, String code) {
        SecurityProperties.Sms sms = properties.getSms();
        String codeKey = CODE_PREFIX + phone;
        String cached = redis.opsForValue().get(codeKey);
        if (cached == null) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        if (!cached.equals(code)) {
            Long fail = redis.opsForValue().increment(FAIL_PREFIX + phone);
            redis.expire(FAIL_PREFIX + phone, sms.getCodeTtl());
            if (fail != null && fail >= sms.getMaxFailCount()) {
                // 错太多次，验证码直接作废，防止暴力枚举 6 位数字
                redis.delete(codeKey);
                redis.delete(FAIL_PREFIX + phone);
            }
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        redis.delete(codeKey);
        redis.delete(FAIL_PREFIX + phone);
    }
}
