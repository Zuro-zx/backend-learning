package top.zsj.security.sms;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 六位数字验证码生成
 * <p>
 * 用 {@code %06d} 而不是 {@code String.valueOf(random)}：后者会把前导零吃掉，
 * "012345" 变成 "12345"，用户照着输反而对不上。
 *
 * @author zsj
 */
public final class SmsCodes {

    private SmsCodes() {
    }

    /**
     * 生成六位数字验证码，保留前导零
     *
     * @return 长度固定为 6 的数字串
     */
    public static String random6Digit() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
