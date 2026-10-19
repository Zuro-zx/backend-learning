package top.zsj.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录出参
 * <p>
 * 一次性把「token + 手机号 + 有效期」返回，前端存 localStorage，
 * 后续请求放在 Authorization: Bearer {token} 头里。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 登录出参
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT 字符串 */
    private String token;

    /** 手机号 */
    private String phone;

    /** 提示语 */
    private String message;

    /** token 有效期（秒） */
    private Long expiresIn;

    public LoginResponse(String token, String phone, long expiresIn) {
        this.token = token;
        this.phone = phone;
        this.message = "登录成功";
        this.expiresIn = expiresIn;
    }
}
