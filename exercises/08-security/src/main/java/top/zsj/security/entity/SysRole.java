package top.zsj.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号的角色与权限点
 * <p>
 * {@code grant_type} 决定 {@code role_code} 要不要补 {@code ROLE_} 前缀：
 * {@link #TYPE_ROLE} 补，{@link #TYPE_PERM} 原样返回。这一行很容易写错，
 * 写错的后果是静默的：{@code hasRole} 永远不通过，日志里只有一行 403。
 *
 * @author zsj
 */
@Data
@TableName("sec_role")
public class SysRole {

    /** 角色：{@code hasRole("ADMIN")} 实际比对 ROLE_ADMIN */
    public static final String TYPE_ROLE = "ROLE";

    /** 权限点：原样比对，适合作细粒度控制，如 ARTICLE_DELETE */
    public static final String TYPE_PERM = "PERM";

    private static final String ROLE_PREFIX = "ROLE_";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String roleCode;

    /** ROLE / PERM，库里默认 ROLE */
    private String grantType;

    /**
     * 转成 Spring Security 的 authority 字符串
     *
     * @return 角色补 {@code ROLE_} 前缀，权限点原样返回
     */
    public String toAuthority() {
        // grant_type 为空时按库里默认值 ROLE 处理，不能直接原样返回，
        // 否则 hasRole 永远比对不上
        return TYPE_PERM.equalsIgnoreCase(grantType) ? roleCode : ROLE_PREFIX + roleCode;
    }
}
