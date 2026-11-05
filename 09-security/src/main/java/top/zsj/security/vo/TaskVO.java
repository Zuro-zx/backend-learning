package top.zsj.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务视图出参（FR-06 / FR-07）
 * <p>
 * 出参白名单：不带 owner_id 这种内部字段，归只用 owner（登录名）表达。
 * 已批改的任务带分数、评语、批改时间；未批改时这三项为 null。
 *
 * @author zsj
 */
@Data
@Builder
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    /** 提交人账号 id，前端做归属比对或调试时用 */
    private Long ownerId;

    /** 提交人登录名 */
    private String owner;

    /** 提交人昵称 */
    private String ownerNickname;

    /** SUBMITTED / REVIEWED */
    private String status;

    /** 状态中文：待批改 / 已批改 */
    private String statusText;

    private Integer score;

    private String comment;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
