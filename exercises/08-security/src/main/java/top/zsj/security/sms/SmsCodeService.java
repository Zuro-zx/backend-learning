package top.zsj.security.sms;

/**
 * 短信验证码服务
 * <p>
 * 两个实现（Redis / 内存）由 {@code zsj.security.sms.store} 二选一装配，
 * {@link SmsCodeAuthenticationProvider} 完全不知道底层是哪个。
 *
 * @author zsj
 */
public interface SmsCodeService {

    /**
     * 发码
     *
     * @param phone 手机号
     * @return 本次生成的验证码；演示模式会把它写进响应
     */
    String sendCode(String phone);

    /**
     * 校验并消费验证码：校验通过立刻作废（一次性）
     *
     * @param phone 手机号
     * @param code  用户提交的验证码
     */
    void verify(String phone, String code);
}
