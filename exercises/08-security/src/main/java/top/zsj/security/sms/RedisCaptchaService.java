package top.zsj.security.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.config.SecurityProperties;
import top.zsj.security.dto.CaptchaResponse;

import java.util.UUID;

/**
 * Redis 版图形验证码
 *
 * @author zsj
 */
@Service
@ConditionalOnProperty(prefix = "zsj.security.sms", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisCaptchaService implements CaptchaService {

    private static final String KEY_PREFIX = "captcha:";

    private final StringRedisTemplate redis;

    private final SecurityProperties properties;

    public RedisCaptchaService(StringRedisTemplate redis, SecurityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public CaptchaResponse create() {
        String uuid = UUID.randomUUID().toString();
        String text = CaptchaImages.randomText();
        redis.opsForValue().set(KEY_PREFIX + uuid, text, properties.getSms().getCaptchaTtl());
        // 演示模式回显答案，本地 curl 不用去猜图；生产 expose-code=false 时为 null
        String answer = properties.getSms().isExposeCode() ? text : null;
        return new CaptchaResponse(uuid, CaptchaImages.toDataUri(text), answer);
    }

    @Override
    public void verify(String uuid, String code) {
        String key = KEY_PREFIX + uuid;
        String expected = redis.opsForValue().get(key);
        if (expected == null || code == null || !expected.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        // 一次性：校验通过立刻删，别让同一个码多用几次
        redis.delete(key);
    }
}
