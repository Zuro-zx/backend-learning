package top.zsj.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建用户请求
 * <p>
 * 新增和修改为什么不用同一个 DTO？
 * 新增时 id 由后端生成、前端不能传；修改时 id 必填。
 * 与其在一个类里用「校验分组」区分两种场景（很容易配错，见 README 的坑记录），
 * 不如拆成两个类，各自的约束一眼就能看清。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 新增用户入参
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 150, message = "年龄不能大于 150")
    private Integer age;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
