package top.zsj.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增账号入参（FR-12）
 * <p>
 * 用户名 3 到 32 位且唯一；密码 6 到 64 位，落库前 BCrypt 加密；
 * 昵称必填；角色至少一个。账号与角色关联写两张表，同一个事务。
 *
 * @author zsj
 */
public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名 3 到 32 位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码 6 到 64 位")
        String password,

        @NotBlank(message = "昵称不能为空")
        String nickname,

        @NotEmpty(message = "角色至少一个")
        List<String> roles,

        /** 启用标记，缺省 true；guest 那样的停用演示账号创建时传 false */
        Boolean enabled) {

    /** enabled 不传就按启用处理 */
    public boolean resolveEnabled() {
        return enabled == null || enabled;
    }
}
