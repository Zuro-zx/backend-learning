package top.zsj.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号实体，对应 sec_user 表
 * <p>
 * password 只存 BCrypt 密文（BR-5）：自带盐，定长 60，列宽留到 100 方便换更长的算法。
 * 实体不出 service 层、不作为接口出参——接口出参一律走 vo 包的白名单视图。
 *
 * @author zsj
 */
@Data
@TableName("sec_user")
public class SecUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一，3 到 32 位 */
    private String username;

    /** BCrypt 密文，明文不落库、不出 service */
    private String password;

    /** 昵称，给人看 */
    private String nickname;

    /** 启用标记：停用后登录返回 30002（演示账号 guest 就是停用状态） */
    private Boolean enabled;
}
