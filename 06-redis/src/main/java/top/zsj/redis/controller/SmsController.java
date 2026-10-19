package top.zsj.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.redis.common.Result;
import top.zsj.redis.dto.LoginRequest;
import top.zsj.redis.dto.LoginResponse;
import top.zsj.redis.service.SmsService;

/**
 * 短信验证码登录接口
 * <p>
 * 1) POST /sms/send?phone=xxx 发验证码（Mock 模式下验证码只看日志/Redis）
 * 2) POST /sms/login 提交验证码 → 返回 JWT
 *
 * @author zsj
 * @date 2026/10/14
 * @description 短信登录控制器
 */
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 统一响应体
     */
    @PostMapping("/send")
    public Result<String> send(@RequestParam String phone) {
        smsService.sendSms(phone);
        return Result.ok("短信发送成功");
    }

    /**
     * 手机号 + 验证码登录
     *
     * @param loginRequest 登录请求体
     * @return token 与有效期
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return Result.ok(smsService.login(loginRequest));
    }
}
