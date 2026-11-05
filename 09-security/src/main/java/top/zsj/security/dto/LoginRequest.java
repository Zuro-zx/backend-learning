package top.zsj.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参
 * <p>
 * 只收用户名与密码。提交人身份永远来自这里校验出的身份，
 * 后续请求里的归属一律取自令牌（BR-1）。
 *
 * @param username 登录名
 * @param password 明文密码，只在登录瞬间出现
 * @author zsj
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
