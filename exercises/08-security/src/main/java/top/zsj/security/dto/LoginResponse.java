package top.zsj.security.dto;

import java.util.List;

/**
 * 登录响应
 *
 * @param token       三段式 JWT
 * @param tokenType   固定 Bearer
 * @param expiresIn   有效期（秒）
 * @param username    登录名
 * @param authorities 权限列表，含 Security 7 自动追加的认证因子（如 FACTOR_PASSWORD）
 * @author zsj
 */
public record LoginResponse(String token, String tokenType, long expiresIn,
                            String username, List<String> authorities) {

    public static LoginResponse of(String token, long expiresIn, String username, List<String> authorities) {
        return new LoginResponse(token, "Bearer", expiresIn, username, authorities);
    }
}
