package top.zsj.security.vo;

import java.util.List;

/**
 * 当前身份出参（FR-02）
 * <p>
 * 从令牌取身份，返回用户名、昵称、角色，不返回密码。
 *
 * @author zsj
 */
public record AuthUserVO(
        String username,
        String nickname,
        List<String> roles) {
}
