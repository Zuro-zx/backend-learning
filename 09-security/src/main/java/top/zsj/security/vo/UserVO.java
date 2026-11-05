package top.zsj.security.vo;

import java.util.List;

/**
 * 账号视图出参（FR-11）
 * <p>
 * 出参是视图对象，密码不出 service——响应里搜不到 password（AC-8）。
 * authorities 是角色摊平后的权限点，前端做按钮级控制时会用到。
 *
 * @author zsj
 */
public record UserVO(
        String username,
        String nickname,
        List<String> roles,
        List<String> authorities,
        Boolean enabled) {
}
