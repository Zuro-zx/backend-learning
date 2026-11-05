package top.zsj.security.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import top.zsj.security.model.Account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * UserDetails 适配
 * <p>
 * 把领域对象 Account 包成 Spring Security 认的 UserDetails。
 * 权限集合 = ROLE_ 前缀的角色 + 角色摊平后的权限点：
 * 角色供 URL 规则（hasRole('ADMIN')）用，权限点供方法级鉴权
 * （hasAuthority('task:review')）用，两套都挂在同一个身份上。
 * <p>
 * 密码密文只在登录校验那一次需要（DaoAuthenticationProvider 比对用），
 * 令牌过滤器重建身份时不需要，所以给了两个构造器。
 *
 * @author zsj
 */
public class SecurityUserDetails implements UserDetails {

    /** 领域对象，控制器里取当前用户用它，不用再查库 */
    private final Account account;

    /** BCrypt 密文，仅登录校验时非空；永不出现在响应里 */
    private final String password;

    /** 登录校验用：带密码密文 */
    public SecurityUserDetails(Account account, String password) {
        this.account = account;
        this.password = password;
    }

    /** 令牌过滤器用：不碰密码 */
    public SecurityUserDetails(Account account) {
        this(account, null);
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 角色：ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN
        for (String role : account.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        // 权限点：task:submit / task:read / task:review / user:read / user:write
        for (String point : account.authorities()) {
            authorities.add(new SimpleGrantedAuthority(point));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return account.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 停用账号登录会抛 DisabledException，Service 转成 30002 */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(account.getEnabled());
    }
}
