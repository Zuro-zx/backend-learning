package top.zsj.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.zsj.security.config.SecurityProperties;
import top.zsj.security.entity.SysUser;
import top.zsj.security.mapper.SysUserMapper;

import java.util.UUID;

/**
 * 短信登录用的 UserDetailsService：按手机号查人
 * <p>
 * 未注册手机号在 {@code auto-register=true} 时自动建号。建号密码列存
 * {@code {noop}} 加一个随机 UUID——任何人都猜不到，等于这个账号只能走短信。
 * 真实业务要在发码前就校验手机号是否已注册，否则知道手机号就能建号。
 *
 * @author zsj
 */
@Slf4j
@Service("phoneUserDetailsService")
public class PhoneUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    private final UserAssembler assembler;

    private final SecurityProperties properties;

    public PhoneUserDetailsService(SysUserMapper userMapper, UserAssembler assembler,
                                   SecurityProperties properties) {
        this.userMapper = userMapper;
        this.assembler = assembler;
        this.properties = properties;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        SysUser user = userMapper.findByPhone(phone);
        if (user == null) {
            if (!properties.getSms().isAutoRegister()) {
                throw new UsernameNotFoundException("手机号未注册：" + mask(phone));
            }
            user = register(phone);
        }
        return assembler.toUserDetails(user);
    }

    /** 首次短信登录自动建号，默认没有任何角色，只能访问"登录即可"的接口 */
    private SysUser register(String phone) {
        SysUser user = new SysUser();
        user.setUsername("user_" + phone);
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        // {noop} 前缀表示明文，用一个随机 UUID 占位，等于这个账号没有可用密码
        user.setPassword("{noop}" + UUID.randomUUID());
        user.setEnabled(true);
        user.setLocked(false);
        userMapper.insert(user);
        log.info("短信首次登录自动建号：username={}, phone={}", user.getUsername(), mask(phone));
        return user;
    }

    private String mask(String phone) {
        return phone.length() == 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : "***";
    }
}
