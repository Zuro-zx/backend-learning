package top.zsj.security.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联表投影：任务 + 提交人账号与昵称
 * <p>
 * 字段与 SecTaskMapper 里的注解 SQL 一一对应，
 * 不依赖 entity 的字段变化，SQL 始终在自己手里。
 *
 * @author zsj
 */
@Data
public class TaskDetail {

    private Long id;

    /** 提交人账号 id，归属判断用 */
    private Long ownerId;

    /** 提交人登录名 */
    private String owner;

    /** 提交人昵称 */
    private String ownerNickname;

    private String title;

    private String content;

    /** SUBMITTED / REVIEWED */
    private String status;

    private Integer score;

    private String comment;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
