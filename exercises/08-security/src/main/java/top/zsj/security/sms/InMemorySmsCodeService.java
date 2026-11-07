package top.zsj.security.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.config.SecurityProperties;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版验证码服务：{@code zsj.security.sms.store=memory} 时装配
 * <p>
 * 不连 Redis 也能跑通整条链路，代价是重启即失效、多实例不共享，只适合本地与测试。
 *
 * @author zsj
 */
@Service
@ConditionalOnProperty(prefix = "zsj.security.sms", name = "store", havingValue = "memory")
public class InMemorySmsCodeService implements SmsCodeService {

    private record Code(String value, Instant expireAt) {
    }

    private final Map<String, Code> codes = new ConcurrentHashMap<>();

    private final Map<String, Instant> limits = new ConcurrentHashMap<>();

    private final Map<String, Integer> failures = new ConcurrentHashMap<>();

    private final SecurityProperties properties;

    public InMemorySmsCodeService(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sendCode(String phone) {
        SecurityProperties.Sms sms = properties.getSms();
        Instant now = Instant.now();

        Instant limit = limits.get(phone);
        if (limit != null && limit.isAfter(now)) {
            throw new BusinessException(ErrorCode.SMS_SEND_TOO_OFTEN);
        }

        String code = SmsCodes.random6Digit();
        codes.put(phone, new Code(code, now.plus(sms.getCodeTtl())));
        limits.put(phone, now.plus(sms.getSendInterval()));
        failures.remove(phone);
        return code;
    }

    @Override
    public void verify(String phone, String code) {
        SecurityProperties.Sms sms = properties.getSms();
        Code cached = codes.get(phone);
        if (cached == null || cached.expireAt().isBefore(Instant.now())) {
            codes.remove(phone);
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        if (!cached.value().equals(code)) {
            int fail = failures.merge(phone, 1, Integer::sum);
            if (fail >= sms.getMaxFailCount()) {
                codes.remove(phone);
                failures.remove(phone);
            }
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        // 一次性：校验通过立刻作废，同一个码不能用两次
        codes.remove(phone);
        failures.remove(phone);
    }
}
