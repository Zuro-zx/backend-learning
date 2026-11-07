package top.zsj.security.sms;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 短信验证码凭证：给 Security 增加一种"手机号 + 验证码"的认证方式
 * <p>
 * 两个构造器对应"登录前"和"登录后"两种状态。{@code super(null)} 必须强转成
 * {@code Collection}——Security 7 的 AbstractAuthenticationToken 有两个接收集合的构造器，
 * 直接写 super(null) 编译器无法判断选哪个，报 "reference to ... is ambiguous"。
 *
 * @author zsj
 */
public class SmsCodeAuthenticationToken extends AbstractAuthenticationToken {

    /** 登录前是手机号，登录后是 UserDetails */
    private final Object principal;

    /** 登录前是验证码，登录后置空 */
    private Object credentials;

    /** 登录前：前端提交的手机号与验证码 */
    public SmsCodeAuthenticationToken(String phone, String code) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = phone;
        this.credentials = code;
        super.setAuthenticated(false);
    }

    /** 认证成功后由 Provider 创建 */
    public SmsCodeAuthenticationToken(Object principal,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        // 必须用 super 调用：本类覆盖了 setAuthenticated，走子类会被拦下来
        super.setAuthenticated(true);
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        // 只允许构造器内部置为已认证，外部不许改
        throw new IllegalArgumentException("不允许手工修改认证状态");
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
