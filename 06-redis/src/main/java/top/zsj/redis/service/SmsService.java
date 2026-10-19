package top.zsj.redis.service;

import top.zsj.redis.dto.LoginRequest;
import top.zsj.redis.dto.LoginResponse;

/**
 * 短信验证码服务
 *
 * @author zsj
 * @date 2026/10/14
 * @description 短信验证码服务
 */
public interface SmsService {

    /**
     * 发送验证码短信。发送失败、校验不通过都通过 BusinessException 往外抛
     *
     * @param phone 手机号
     */
    void sendSms(String phone);

    /**
     * 验证码登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应（token + 有效期）
     */
    LoginResponse login(LoginRequest loginRequest);
}
