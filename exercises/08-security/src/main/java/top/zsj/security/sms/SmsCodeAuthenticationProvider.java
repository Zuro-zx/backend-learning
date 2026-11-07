package top.zsj.security.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 短信验证码认证器
 * <p>
 * 最容易被忽略的一段是账号状态检查：{@code DaoAuthenticationProvider} 是替密码登录做的，
 * 不会替这里做。漏掉这段，被禁用或锁定的账号照样能靠短信登进来。
 *
 * @author zsj
 */
@Slf4j
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {

    private final SmsCodeService smsCodeService;

    private final UserDetailsService phoneUserDetailsService;

    public SmsCodeAuthenticationProvider(SmsCodeService smsCodeService,
                                         UserDetailsService phoneUserDetailsService) {
        this.smsCodeService = smsCodeService;
        this.phoneUserDetailsService = phoneUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String phone = (String) authentication.getPrincipal();
        String code = (String) authentication.getCredentials();

        // 1. 校验验证码（一次性，错误次数由 SmsCodeService 控制）
        smsCodeService.verify(phone, code);

        // 2. 按手机号查人（未注册且开启 auto-register 时自动建号）
        UserDetails user = phoneUserDetailsService.loadUserByUsername(phone);

        // 3. 账号状态检查必须自己做
        if (!user.isEnabled()) {
            throw new DisabledException("账号已被禁用");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("账号已被锁定");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("账号已过期");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("凭证已过期");
        }

        log.info("短信登录成功：phone={}", mask(phone));

        SmsCodeAuthenticationToken result =
                new SmsCodeAuthenticationToken(user, user.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String mask(String phone) {
        return phone.length() == 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "***";
    }
}
