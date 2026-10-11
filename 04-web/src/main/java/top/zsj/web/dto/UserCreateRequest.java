package top.zsj.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建用户请求（DTO）
 * <p>
 * 为什么新增时不直接收 User 实体？
 * 1. id、birthday、address 都是后端生成的，实体里有这些字段等于告诉前端「可以传」；
 * 2. DTO 上可以挂校验注解，实体上挂了会影响内部使用。
 * 所以对外入参单独建类，只暴露「前端确实该传」的字段，并做好必备校验。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 创建用户请求
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 150, message = "年龄不能大于 150")
    private Integer age;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
