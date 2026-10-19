package top.zsj.zhihu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知乎专题实体，对应 t_special 表
 * <p>
 * 字段与表列一一对应，靠 application.yml 里的 map-underscore-to-camel-case
 * 把 is_following / followers_count 这类下划线列名映射到驼峰属性。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 知乎专题实体
 */
@Data
@TableName("t_special")
public class Special {

    /**
     * 专题 ID，来源于知乎接口，不是自增主键，插入时由外部提供。
     * <p>
     * 用 String 承接 BIGINT：知乎的 id 是 19 位，超出 JS Number 的安全整数范围
     * （2^53 ≈ 9.007e15），用 Long 序列化成数字给前端会丢精度
     * （2086116797393257830 → 2086116797393257800）。MyBatis 读列时走 getString，
     * 类型是字符串也不影响查询。
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 标题 */
    private String title;

    /** 简介 */
    private String description;

    /** 分类，扩展练习 1 新增，库里由关键词规则打标：传统节日 / 网络文明 / 阅读文化 / 科技前沿 / 社会民生 */
    private String category;

    /** 封面图地址 */
    private String banner;

    /** 是否关注，库里存 TRUE / FALSE 字符串，前端据此切换两种样式 */
    private String isFollowing;

    /** 关注人数 */
    private Integer followersCount;

    /** 浏览人数 */
    private Integer viewCount;

    /** 更新时间，库里是 DATETIME，序列化成 yyyy-MM-dd HH:mm:ss 给前端直接显示 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updated;
}
