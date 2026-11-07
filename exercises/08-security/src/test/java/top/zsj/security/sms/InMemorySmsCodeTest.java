package top.zsj.security.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.config.SecurityProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 内存版验证码服务：6 个用例
 * <p>
 * 用内存实现测，不依赖 Redis；发送间隔调到毫秒级，否则测一次要等一分钟。
 *
 * @author zsj
 */
class InMemorySmsCodeTest {

    private static final String PHONE = "13900001111";

    private SecurityProperties properties;

    private InMemorySmsCodeService service;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getSms().setSendInterval(Duration.ofMillis(50));
        properties.getSms().setCodeTtl(Duration.ofMillis(300));
        properties.getSms().setMaxFailCount(3);
        service = new InMemorySmsCodeService(properties);
    }

    @Test
    @DisplayName("发码后能用同一个码校验通过")
    void shouldVerifyWithSentCode() {
        String code = service.sendCode(PHONE);

        assertThat(code).matches("\\d{6}");
        service.verify(PHONE, code);
    }

    @Test
    @DisplayName("验证码是一次性的，第二次校验必定失败")
    void shouldRejectReusedCode() {
        String code = service.sendCode(PHONE);
        service.verify(PHONE, code);

        assertThatThrownBy(() -> service.verify(PHONE, code))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SMS_CODE_INVALID);
    }

    @Test
    @DisplayName("连错达到上限后验证码作废，之后输对也不行")
    void shouldInvalidateAfterTooManyFailures() {
        String code = service.sendCode(PHONE);

        for (int i = 0; i < properties.getSms().getMaxFailCount(); i++) {
            String wrong = "000000".equals(code) ? "111111" : "000000";
            assertThatThrownBy(() -> service.verify(PHONE, wrong))
                    .isInstanceOf(BusinessException.class);
        }

        assertThatThrownBy(() -> service.verify(PHONE, code))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("发送冷却期内重复发码被拒")
    void shouldRejectSendWithinInterval() {
        service.sendCode(PHONE);

        assertThatThrownBy(() -> service.sendCode(PHONE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SMS_SEND_TOO_OFTEN);
    }

    @Test
    @DisplayName("过了有效期后校验失败")
    void shouldRejectExpiredCode() throws InterruptedException {
        properties.getSms().setCodeTtl(Duration.ofMillis(30));
        InMemorySmsCodeService shortLived = new InMemorySmsCodeService(properties);
        String code = shortLived.sendCode(PHONE);

        Thread.sleep(60);

        assertThatThrownBy(() -> shortLived.verify(PHONE, code))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("验证码固定 6 位，且保留前导零")
    void shouldKeepLeadingZeros() {
        boolean hasLeadingZero = false;
        for (int i = 0; i < 2000; i++) {
            String code = SmsCodes.random6Digit();
            assertThat(code).matches("\\d{6}");
            if (code.startsWith("0")) {
                hasLeadingZero = true;
            }
        }
        // 2000 次里一次前导零都没有，概率约 0.1^2000，说明实现把零吃掉了
        assertThat(hasLeadingZero).isTrue();
    }
}
