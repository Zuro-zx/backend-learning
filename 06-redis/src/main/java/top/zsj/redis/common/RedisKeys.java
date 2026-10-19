package top.zsj.redis.common;

/**
 * Redis key 命名集中管理
 * <p>
 * 每个 key 的含义写在一处，避免「这头写 sms:captcha:、那头写 sms:code:」读不到数据；
 * 也方便在 redis-cli 里按冒号分层查看。
 *
 * @author zsj
 * @date 2026/10/19
 * @description Redis key 命名
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * 验证码 Key，存 Hash：code 是验证码，status 是 0 未使用 / 1 已使用
     *
     * @param phone 手机号
     * @return key
     */
    public static String getSmsKey(String phone) {
        return "sms:captcha:" + phone;
    }

    /**
     * 发送间隔 Key，存在即代表还在冷却中
     *
     * @param phone 手机号
     * @return key
     */
    public static String getSmsSendIntervalKey(String phone) {
        return "sms:send:interval:" + phone;
    }

    /**
     * 当日发送次数 Key
     *
     * @param phone 手机号
     * @return key
     */
    public static String getSmsSendCountKey(String phone) {
        return "sms:send:count:" + phone;
    }

    /**
     * 验证码连续输错次数 Key
     *
     * @param phone 手机号
     * @return key
     */
    public static String getSmsFailCountKey(String phone) {
        return "sms:fail:" + phone;
    }
}
