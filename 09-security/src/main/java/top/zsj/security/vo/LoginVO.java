package top.zsj.security.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录结果出参（FR-01）
 * <p>
 * 返回令牌、过期时间、昵称、角色。令牌是无状态的，
 * 退出没有后端接口（FR-03），前端只清存储。
 *
 * @author zsj
 */
public record LoginVO(
        String token,
        String tokenType,
        LocalDateTime expiresAt,
        String username,
        String nickname,
        List<String> roles) {
}
