package top.zsj.security.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.zsj.security.entity.SecUser;
import top.zsj.security.mapper.SecUserMapper;
import top.zsj.security.mapper.SecUserRoleMapper;
import top.zsj.security.model.Account;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * UserDetailsService 实现：从库里把账号与角色装成身份
 * <p>
 * 登录时 DaoAuthenticationProvider 调它取 UserDetails 做密码比对；
 * 令牌过滤器每个请求也会调它重建身份，保证「停用账号的旧令牌也进不来」。
 * 查不到账号抛 UsernameNotFoundException，登录出口统一转 30001，
 * 不告诉对方账号是否存在。
 *
 * @author zsj
 */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final SecUserMapper userMapper;
    private final SecUserRoleMapper userRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SecUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在：" + username);
        }
        List<String> roleCodes = userRoleMapper.selectRoleCodes(user.getId());
        Account account = Account.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .enabled(user.getEnabled())
                .roles(new LinkedHashSet<>(roleCodes))
                .build();
        return new SecurityUserDetails(account, user.getPassword());
    }
}
