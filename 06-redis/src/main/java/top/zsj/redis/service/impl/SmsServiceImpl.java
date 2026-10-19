package top.zsj.redis.service.impl;

import com.cloopen.rest.sdk.BodyType;
import com.cloopen.rest.sdk.CCPRestSmsSDK;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.zsj.redis.common.BusinessException;
import top.zsj.redis.common.CommonUtils;
import top.zsj.redis.common.ErrorCode;
import top.zsj.redis.common.JwtUtils;
import top.zsj.redis.common.RedisCache;
import top.zsj.redis.common.RedisKeys;
import top.zsj.redis.config.CloopenConfig;
import top.zsj.redis.config.SmsProperties;
import top.zsj.redis.dto.LoginRequest;
import top.zsj.redis.dto.LoginResponse;
import top.zsj.redis.service.SmsService;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信验证码服务实现
 * <p>
 * 一次登录分两个阶段，中间靠 Redis 传递验证码：
 * 阶段一 sendSms：校验手机号 → 抢发送间隔锁 → 当日计数 → 生成验证码 → 发送 → 写 Hash；
 * 阶段二 login：读 Hash → 判空/判已用/判对错 → 标记已用 → 签发 JWT。
 * <p>
 * 三个顺序上的讲究：
 * <ul>
 *   <li>先抢间隔锁再干别的：锁抢不到说明 60 秒内刚发过，后面都不用做；</li>
 *   <li>先发短信再写 Redis：反过来的话短信没发出去却留个用户永远收不到的验证码；</li>
 *   <li>发送失败要删掉间隔锁：否则用户得白等 60 秒才能重试。</li>
 * </ul>
 *
 * @author zsj
 * @date 2026/10/14
 * @description 短信验证码服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    /** 一天有多少秒，用于「当日发送条数」的计数窗口 */
    private static final long SECONDS_PER_DAY = 24 * 60 * 60L;

    /** 验证码 Hash 里存验证码的字段 */
    private static final String FIELD_CODE = "code";

    /** 验证码 Hash 里存状态的字段 */
    private static final String FIELD_STATUS = "status";

    /** 未使用 */
    private static final String STATUS_UNUSED = "0";

    /** 已使用 */
    private static final String STATUS_USED = "1";

    private final CloopenConfig cloopenConfig;
    private final SmsProperties smsProperties;
    private final RedisCache redisCache;
    private final JwtUtils jwtUtils;

    @Override
    public void sendSms(String phone) {
        // 1、手机号格式
        if (!CommonUtils.checkPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ERROR);
        }

        // 2、发送间隔：SET NX EX 只让一次请求占住这个 key，占不到说明刚发过
        String intervalKey = RedisKeys.getSmsSendIntervalKey(phone);
        Boolean acquired = redisCache.setIfAbsent(intervalKey, 1, smsProperties.getSendIntervalSeconds());
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.PHONE_CODE_SEND_TOO_MANY);
        }

        // 3、当日条数：计数器过期时间在第一次自增时设置
        Long sentToday = redisCache.increment(RedisKeys.getSmsSendCountKey(phone), SECONDS_PER_DAY);
        if (sentToday != null && sentToday > smsProperties.getSendDailyLimit()) {
            throw new BusinessException(ErrorCode.PHONE_CODE_SEND_LIMIT);
        }

        // 4、生成 4 位随机验证码
        int code = CommonUtils.generateCode();

        // 5、先发短信，成功之后再写 Redis：用户没收到短信时不留验证码占位
        if (!send(phone, code)) {
            // 发送失败，撤掉冷却锁，允许立刻重试
            redisCache.delete(intervalKey);
            throw new BusinessException(ErrorCode.PHONE_CODE_SEND_FAILED);
        }

        // 6、验证码与状态一起存成 Hash，方便区分「没用过」和「已经用过了」
        Map<String, Object> value = new HashMap<>(4);
        value.put(FIELD_CODE, String.valueOf(code));
        value.put(FIELD_STATUS, STATUS_UNUSED);
        redisCache.hMSet(RedisKeys.getSmsKey(phone), value, smsProperties.getCodeExpireSeconds());

        log.info("验证码已下发，手机号 {}", phone);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String phone = loginRequest.getPhone();
        String inputCode = loginRequest.getCode();

        // 1、手机号格式
        if (!CommonUtils.checkPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ERROR);
        }

        // 2、验证码非空
        if (inputCode == null || inputCode.isBlank()) {
            throw new BusinessException(ErrorCode.PHONE_CODE_EMPTY);
        }

        // 3、取验证码，先判空再取值
        String redisKey = RedisKeys.getSmsKey(phone);
        Map<String, Object> cached = redisCache.hGetAll(redisKey);
        if (cached.isEmpty()) {
            throw new BusinessException(ErrorCode.PHONE_CODE_EXPIRED);
        }

        // 4、已经用过的验证码不能再登录一次
        if (STATUS_USED.equals(cached.get(FIELD_STATUS))) {
            throw new BusinessException(ErrorCode.PHONE_CODE_USED);
        }

        // 5、验证码不匹配：连错到上限就作废，防止 4 位验证码被逐个试
        if (!inputCode.equals(cached.get(FIELD_CODE))) {
            Long failCount = redisCache.increment(
                    RedisKeys.getSmsFailCountKey(phone), smsProperties.getCodeExpireSeconds());
            if (failCount != null && failCount >= smsProperties.getMaxFailCount()) {
                redisCache.delete(redisKey);
                redisCache.delete(RedisKeys.getSmsFailCountKey(phone));
                log.warn("手机号 {} 验证码连续输错 {} 次，已作废", phone, failCount);
                throw new BusinessException(ErrorCode.PHONE_CODE_ERROR_TOO_MANY);
            }
            throw new BusinessException(ErrorCode.PHONE_CODE_ERROR);
        }

        // 6、验证通过：状态改成已使用并保留到过期，重复提交能给出明确提示
        redisCache.hSet(redisKey, FIELD_STATUS, STATUS_USED, smsProperties.getCodeExpireSeconds());
        redisCache.delete(RedisKeys.getSmsFailCountKey(phone));

        // 7、签发 token
        String token = jwtUtils.create(phone);
        log.info("手机号 {} 登录成功", phone);
        return new LoginResponse(token, phone, jwtUtils.getExpireSeconds());
    }

    /**
     * 调用短信通道。mockEnabled 为 true 时走模拟通道，验证码只写日志，不真正发送
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否发送成功
     */
    private boolean send(String phone, int code) {
        if (smsProperties.isMockEnabled()) {
            log.info("短信通道为模拟模式，手机号 {}，验证码 {}", phone, code);
            return true;
        }

        CCPRestSmsSDK sdk = new CCPRestSmsSDK();
        sdk.init(cloopenConfig.getServerIp(), String.valueOf(cloopenConfig.getPort()));
        sdk.setAccount(cloopenConfig.getAccountSid(), cloopenConfig.getAccountToken());
        sdk.setAppId(cloopenConfig.getAppId());
        sdk.setBodyType(BodyType.Type_JSON);

        String[] dataS = {String.valueOf(code)};
        HashMap<String, Object> result = sdk.sendTemplateSMS(phone, cloopenConfig.getTemplateId(), dataS);

        if ("000000".equals(result.get("statusCode"))) {
            log.info("短信发送成功，手机号 {}", phone);
            return true;
        }
        log.warn("短信发送失败，手机号 {}，状态码 {}，状态信息 {}",
                phone, result.get("statusCode"), result.get("statusMsg"));
        return false;
    }
}
