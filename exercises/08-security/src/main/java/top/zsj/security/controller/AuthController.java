package top.zsj.security.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;
import top.zsj.security.dto.ChangePasswordRequest;
import top.zsj.security.dto.LoginRequest;
import top.zsj.security.dto.LoginResponse;
import top.zsj.security.dto.UserInfo;
import top.zsj.security.service.AuthService;

import java.util.List;

/**
 * 密码登录、当前用户、登出、改密
 *
 * @author zsj
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 密码登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** 当前登录用户：把 token 里的身份回显出来，前端刷新页面后用它恢复状态 */
    @GetMapping("/me")
    public Result<UserInfo> me(Authentication authentication) {
        return Result.ok(new UserInfo(authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()));
    }

    /** 登出：删掉白名单里的 jti，token 立刻失效（练习 2） */
    @PostMapping("/logout")
    public Result<Void> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return Result.ok();
    }

    /** 改密：改完该账号所有 token 一起作废（练习 2 的场景） */
    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(),
                request.oldPassword(), request.newPassword());
        return Result.ok();
    }
}
