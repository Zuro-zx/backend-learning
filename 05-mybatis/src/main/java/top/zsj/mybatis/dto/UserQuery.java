package top.zsj.mybatis.dto;

import lombok.Data;

/**
 * 用户查询条件，字段全为空时等价于查全部
 *
 * @author zsj
 */
@Data
public class UserQuery {

    /** 匹配 username 或 nickname */
    private String keyword;

    private Integer gender;

    private String memberLevel;

    private Boolean vip;

    private Integer status;

    private Integer minAge;

    private Integer maxAge;
}
