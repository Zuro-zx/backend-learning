package top.zsj.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;
import top.zsj.security.dto.LoginRequest;
import top.zsj.security.security.SecurityUserDetails;
import top.zsj.security.service.AuthService;
import top.zsj.security.vo.AuthUserVO;
import top.zsj.security.vo.LoginVO;

/**
 * 认证接口
 * <p>
 * /auth/login 匿名可访问（SecurityConfig 里放行），/auth/me 登录即可。
 * 退出没有后端接口（FR-03）：令牌无状态，到期前仍然有效，前端只清存储。
 *
 * @author zsj
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** FR-01 登录换令牌 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** FR-02 查当前身份：从令牌取身份，不返回密码 */
    @GetMapping("/me")
    public Result<AuthUserVO> me(@AuthenticationPrincipal SecurityUserDetails principal) {
        return Result.ok(authService.me(principal.getAccount()));
    }
}
