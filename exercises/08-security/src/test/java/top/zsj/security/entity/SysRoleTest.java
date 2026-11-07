package top.zsj.security.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ROLE_ 前缀规则：3 个用例
 * <p>
 * 这一行 if 写错的后果是静默的：hasRole 永远不通过，日志里只有一行 403，看不出原因。
 *
 * @author zsj
 */
class SysRoleTest {

    private SysRole role(String code, String grantType) {
        SysRole r = new SysRole();
        r.setRoleCode(code);
        r.setGrantType(grantType);
        return r;
    }

    @Test
    @DisplayName("grant_type=ROLE 时补 ROLE_ 前缀")
    void shouldPrefixRoleType() {
        assertThat(role("ADMIN", SysRole.TYPE_ROLE).toAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("grant_type=PERM 时原样返回，不补前缀")
    void shouldKeepPermissionTypeAsIs() {
        assertThat(role("ARTICLE_DELETE", SysRole.TYPE_PERM).toAuthority()).isEqualTo("ARTICLE_DELETE");
    }

    @Test
    @DisplayName("grant_type 为空时按 ROLE 处理，仍然补前缀")
    void shouldTreatNullGrantTypeAsRole() {
        assertThat(role("USER", null).toAuthority()).isEqualTo("ROLE_USER");
        assertThat(role("ADMIN", "").toAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
