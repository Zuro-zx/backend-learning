package top.zsj.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.zsj.security.entity.SysUser;
import top.zsj.security.mapper.SysUserMapper;

/**
 * 密码登录用的 UserDetailsService：按登录名查人
 * <p>
 * 容器里一旦有了 UserDetailsService，Boot 就不再生成默认用户，启动日志里也不会再出现
 * "Using generated security password"。
 *
 * @author zsj
 */
@Service("dbUserDetailsService")
public class DbUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    private final UserAssembler assembler;

    public DbUserDetailsService(SysUserMapper userMapper, UserAssembler assembler) {
        this.userMapper = userMapper;
        this.assembler = assembler;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            // 抛这个消息不会泄露给前端：DaoAuthenticationProvider 默认把它兜成 BadCredentialsException
            throw new UsernameNotFoundException("账号不存在：" + username);
        }
        return assembler.toUserDetails(user);
    }
}
