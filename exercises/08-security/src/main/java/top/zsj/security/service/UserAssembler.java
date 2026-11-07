package top.zsj.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import top.zsj.security.entity.SysRole;
import top.zsj.security.entity.SysUser;
import top.zsj.security.mapper.SysRoleMapper;

import java.util.List;

/**
 * 账号 → UserDetails 装配
 * <p>
 * 密码登录与短信登录共用同一段装配逻辑，权限来源只有一处，避免两条路各查一遍还查得不一样。
 *
 * @author zsj
 */
@Component
public class UserAssembler {

    private final SysRoleMapper roleMapper;

    public UserAssembler(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /** 权限对象列表，交给 Security 做匹配 */
    public List<GrantedAuthority> authoritiesOf(Long userId) {
        return roleMapper.findByUserId(userId).stream()
                .map(SysRole::toAuthority)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
    }

    /** 权限字符串列表，写进 JWT payload */
    public List<String> authorityNamesOf(Long userId) {
        return roleMapper.findByUserId(userId).stream()
                .map(SysRole::toAuthority)
                .toList();
    }

    /**
     * 装配 UserDetails
     * <p>
     * {@code .disabled()} 与 {@code .accountLocked()} 是账号状态的来源，
     * DaoAuthenticationProvider 会在校验密码之前检查这两个标记。
     */
    public UserDetails toUserDetails(SysUser user) {
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authoritiesOf(user.getId()))
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .accountLocked(Boolean.TRUE.equals(user.getLocked()))
                .build();
    }
}
