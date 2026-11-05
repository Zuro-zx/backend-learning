package top.zsj.security.model;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 领域对象：登录账号（不依赖表结构变化）
 * <p>
 * 由 DbUserDetailsService 从 sec_user + 关联角色组装，
 * 被 SecurityUserDetails 包着放进 SecurityContext，
 * 控制器与 Service 里拿到的「当前用户」就是它。
 * 归属判断（BR-1）用的 id 从这里取，永远不从请求体取。
 *
 * @author zsj
 */
@Data
@Builder
public class Account {

    /** 账号 id，归属判断的唯一依据 */
    private Long id;

    /** 登录名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 是否启用 */
    private Boolean enabled;

    /** 角色编码集合，如 [STUDENT] */
    private Set<String> roles;

    /** 摊平后的权限点集合：角色枚举里的 authorities 全部并起来 */
    public Set<String> authorities() {
        Set<String> result = new LinkedHashSet<>();
        if (roles == null) {
            return result;
        }
        for (String code : roles) {
            RoleCode role = RoleCode.fromCode(code);
            if (role != null) {
                result.addAll(java.util.Arrays.asList(role.getAuthorities()));
            }
        }
        return result;
    }

    /** 是否持有某个权限点 */
    public boolean hasAuthority(String authority) {
        return authorities().contains(authority);
    }
}
