package top.zsj.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体，对应 sec_role 表
 * <p>
 * code 与 RoleCode 枚举同名，种子数据写死三条：STUDENT / TEACHER / ADMIN。
 *
 * @author zsj
 */
@Data
@TableName("sec_role")
public class SecRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，唯一，与枚举同名 */
    private String code;

    /** 角色显示名，给人看 */
    private String name;
}
