package top.zsj.web.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户实体
 * <p>
 * 实体对内（对应数据库表字段），DTO 对外（接口入参）。
 * id、birthday、address 由后端生成/补充，前端不用传也传不进来。
 *
 * @author zsj
 * @date 2026/9/23
 * @description 用户实体
 */
@Data
public class User {

    private Long id;
    private String username;
    private Integer age;
    private String email;
    private LocalDate birthday;
    private String address;
}
