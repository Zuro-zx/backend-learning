package top.zsj.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.dto.LoginRequest;
import top.zsj.security.dto.LoginResponse;
import top.zsj.security.dto.SmsLoginRequest;
import top.zsj.security.entity.SysUser;
import top.zsj.security.mapper.SysUserMapper;
import top.zsj.security.security.JwtService;
import top.zsj.security.security.TokenRegistry;
import top.zsj.security.sms.SmsCodeAuthenticationToken;

import java.util.List;

/**
 * 登录 / 登出 / 改密
 * <p>
 * 两个登录入口共用一段签发逻辑：认证交给 AuthenticationManager，签发只写一遍。
 * 账号不可用的异常要分开转：密码错是"你输错了"，禁用/锁定是"你该去找管理员"，
 * 混成一个 401 用户会一直重试密码。
 *
 * @author zsj
 */
@Slf4j
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final TokenRegistry tokenRegistry;

    private final SysUserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       TokenRegistry tokenRegistry, SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenRegistry = tokenRegistry;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 密码登录 */
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
            return issue(authentication);
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (AccountExpiredException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXPIRED);
        } catch (CredentialsExpiredException e) {
            throw new BusinessException(ErrorCode.CREDENTIALS_EXPIRED);
        } catch (AuthenticationException e) {
            // 账号不存在与密码错误在这里合并成同一句话，不告诉对方账号是否存在
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
    }

    /** 短信登录：凭证类型换成 SmsCodeAuthenticationToken，走的还是同一个 AuthenticationManager */
    public LoginResponse smsLogin(SmsLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new SmsCodeAuthenticationToken(request.phone(), request.code()));
            return issue(authentication);
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, e.getMessage());
        }
    }

    /**
     * 签发 token 并登记白名单
     * <p>
     * 练习 2 / 3：签发时把 jti 写进 Redis，同一账号只保留最新一个，
     * 所以新登录会把旧 token 顶掉。
     */
    private LoginResponse issue(Authentication authentication) {
        String username = authentication.getName();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtService.IssuedToken issued = jwtService.issue(username, authorities);
        tokenRegistry.issue(username, issued.jti(), issued.ttl());

        log.info("登录成功：username={}, authorities={}", username, authorities);
        return LoginResponse.of(issued.token(), jwtService.getExpiresInSeconds(), username, authorities);
    }

    /** 登出：删掉白名单里的 jti，token 立刻作废 */
    public void logout(String username) {
        tokenRegistry.revoke(username);
        log.info("登出：username={}", username);
    }

    /**
     * 改密码：改完把该账号所有 token 作废
     * <p>
     * 教程里"改了密码也拦不住"的漏洞就是这一步——token 一旦签发，到期之前都有效。
     *
     * @param username    当前登录用户
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        // 库里的密文带 {bcrypt} 前缀，必须用 DelegatingPasswordEncoder 校验，不能直接 BCrypt.matches
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        tokenRegistry.revoke(username);
        log.info("改密成功，已作废该账号全部 token：username={}", username);
    }
}
