package top.zsj.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户请求
 * <p>
 * 与 UserCreateRequest 的唯一区别：多一个必填的 id。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 修改用户入参
 */
@Data
public class UserUpdateRequest {

    @NotNull(message = "修改时 id 不能为空")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 150, message = "年龄不能大于 150")
    private Integer age;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
