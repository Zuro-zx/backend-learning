package top.zsj.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新用户请求（DTO）
 * <p>
 * 与 UserCreateRequest 的区别：id 放在路径上（PUT /users/{id}），
 * body 里只收本次允许修改的字段（用户名、邮箱），
 * 不存在「传完整实体」的必要 —— 想改什么就暴露什么。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 更新用户请求
 */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
