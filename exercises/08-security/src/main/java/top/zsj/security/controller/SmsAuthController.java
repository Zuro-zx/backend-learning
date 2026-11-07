package top.zsj.security.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.common.Result;
import top.zsj.security.config.SecurityProperties;
import top.zsj.security.dto.CaptchaResponse;
import top.zsj.security.dto.LoginResponse;
import top.zsj.security.dto.SmsLoginRequest;
import top.zsj.security.service.AuthService;
import top.zsj.security.sms.CaptchaService;
import top.zsj.security.sms.SmsCodeService;

/**
 * 短信登录：发码 + 换 token
 * <p>
 * 两个接口都很薄——校验验证码、查人、检查账号状态都在
 * {@link top.zsj.security.sms.SmsCodeAuthenticationProvider} 里，走的还是同一个
 * AuthenticationManager，不是另写一套登录逻辑。
 *
 * @author zsj
 */
@Validated
@RestController
@RequestMapping("/api/auth")
public class SmsAuthController {

    private final SmsCodeService smsCodeService;

    private final CaptchaService captchaService;

    private final AuthService authService;

    private final SecurityProperties properties;

    public SmsAuthController(SmsCodeService smsCodeService, CaptchaService captchaService,
                             AuthService authService, SecurityProperties properties) {
        this.smsCodeService = smsCodeService;
        this.captchaService = captchaService;
        this.authService = authService;
        this.properties = properties;
    }

    /** 练习 5：先取图，答案存在服务端 */
    @GetMapping("/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.ok(captchaService.create());
    }

    /**
     * 发码
     * <p>
     * 练习 5 之后多了一步图形验证码：不带 captchaUuid / captchaCode 直接拒绝，
     * 这样脚本没法只靠一个手机号列表就把短信刷爆。
     * <p>
     * {@code @Pattern} 写在 {@code @RequestParam} 上，类上必须有 {@code @Validated} 才生效。
     */
    @PostMapping("/sms/code")
    public Result<String> sendCode(
            @RequestParam
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @RequestParam(required = false) String captchaUuid,
            @RequestParam(required = false) String captchaCode) {

        if (captchaUuid == null || captchaUuid.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID, "请先获取并填写图形验证码");
        }
        captchaService.verify(captchaUuid, captchaCode);

        String code = smsCodeService.sendCode(phone);
        return Result.ok(properties.getSms().isExposeCode()
                ? "验证码已发送（演示模式，验证码：" + code + "）"
                : "验证码已发送");
    }

    /** 用验证码换 token */
    @PostMapping("/sms/login")
    public Result<LoginResponse> login(@Valid @RequestBody SmsLoginRequest request) {
        return Result.ok(authService.smsLogin(request));
    }
}
