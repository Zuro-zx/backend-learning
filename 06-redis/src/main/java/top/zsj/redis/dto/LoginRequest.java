package top.zsj.redis.dto;

import lombok.Data;

/**
 * 登录入参
 * <p>
 * 这里不加 @Valid：校验规则写在 Service 里，返回的是业务码（40001/40003），
 * 而不是框架的 400，前端只需要认一套业务码。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 登录入参
 */
@Data
public class LoginRequest {

    /** 手机号 */
    private String phone;

    /** 短信验证码 */
    private String code;
}
