package top.zsj.security.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号-角色关联实体，对应 sec_user_role 表
 * <p>
 * 多对多，主键是 (user_id, role_id) 两列的组合，没有自增 id。
 * 删账号必须先删这里再删 sec_user，两步同一个事务。
 *
 * @author zsj
 */
@Data
@TableName("sec_user_role")
public class SecUserRole {

    /** 账号 id，指向 sec_user.id */
    private Long userId;

    /** 角色 id，指向 sec_role.id */
    private Long roleId;
}
