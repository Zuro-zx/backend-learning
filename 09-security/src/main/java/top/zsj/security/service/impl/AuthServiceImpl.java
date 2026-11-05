package top.zsj.security.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.dto.LoginRequest;
import top.zsj.security.model.Account;
import top.zsj.security.security.JwtTokenProvider;
import top.zsj.security.security.SecurityUserDetails;
import top.zsj.security.service.AuthService;
import top.zsj.security.vo.AuthUserVO;
import top.zsj.security.vo.LoginVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务实现
 * <p>
 * 两个分支必须分得开（AC-7）：
 * 密码错返回 30001，账号停用返回 30002，提示语不同。
 * 账号不存在与密码错都归 30001、文案一样——不告诉对方账号是否存在。
 *
 * @author zsj
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            // 密码对了但 enabled 为假
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            // 账号不存在与密码不对走同一个出口
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        SecurityUserDetails details = (SecurityUserDetails) authentication.getPrincipal();
        Account account = details.getAccount();
        List<String> roles = new ArrayList<>(account.getRoles());
        String token = tokenProvider.generate(account.getId(), account.getUsername(), roles);
        return new LoginVO(token, "Bearer", tokenProvider.expiresAt(),
                account.getUsername(), account.getNickname(), roles);
    }

    @Override
    public AuthUserVO me(Account account) {
        return new AuthUserVO(account.getUsername(), account.getNickname(),
                new ArrayList<>(account.getRoles()));
    }
}
