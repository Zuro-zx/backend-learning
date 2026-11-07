package top.zsj.security.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.config.SecurityProperties;
import top.zsj.security.dto.CaptchaResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版图形验证码
 *
 * @author zsj
 */
@Service
@ConditionalOnProperty(prefix = "zsj.security.sms", name = "store", havingValue = "memory")
public class InMemoryCaptchaService implements CaptchaService {

    private record Entry(String text, Instant expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private final SecurityProperties properties;

    public InMemoryCaptchaService(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public CaptchaResponse create() {
        String uuid = UUID.randomUUID().toString();
        String text = CaptchaImages.randomText();
        store.put(uuid, new Entry(text, Instant.now().plus(properties.getSms().getCaptchaTtl())));
        // 演示模式回显答案，本地 curl 不用去猜图；生产 expose-code=false 时为 null
        String answer = properties.getSms().isExposeCode() ? text : null;
        return new CaptchaResponse(uuid, CaptchaImages.toDataUri(text), answer);
    }

    @Override
    public void verify(String uuid, String code) {
        Entry entry = store.get(uuid);
        if (entry == null || entry.expireAt().isBefore(Instant.now())) {
            store.remove(uuid);
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        if (code == null || !entry.text().equalsIgnoreCase(code.trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        store.remove(uuid);
    }
}
