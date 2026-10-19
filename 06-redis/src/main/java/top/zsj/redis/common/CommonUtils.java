package top.zsj.redis.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 通用工具：手机号校验、随机验证码
 *
 * @author zsj
 * @date 2026/10/19
 * @description 通用工具类
 */
public final class CommonUtils {

    /** 中国大陆手机号：11 位，1 开头，第二位 3-9 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private CommonUtils() {
    }

    /**
     * 校验手机号格式
     *
     * @param phone 手机号
     * @return 是否合法
     */
    public static boolean checkPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 生成 4 位随机验证码，范围 [1000, 9999]
     * <p>
     * nextInt 的右边界是开区间，写 9999 会让 9999 永远取不到，所以要写 10000。
     *
     * @return 验证码
     */
    public static int generateCode() {
        return ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
