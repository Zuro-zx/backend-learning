package top.zsj.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务提交实体，对应 sec_task 表
 * <p>
 * 归属指向 sec_user.id（BR-2：存 owner_id 不存用户名，用户名将来可能改）。
 * 状态只有待批改与已批改两种，见 TaskStatus 枚举。
 *
 * @author zsj
 */
@Data
@TableName("sec_task")
public class SecTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人账号 id，永远取自令牌（BR-1），请求体里的 owner 一律忽略 */
    private Long ownerId;

    /** 任务标题，非空且不超过 100 字 */
    private String title;

    /** 任务内容，非空且不超过 2000 字 */
    private String content;

    /** 状态：SUBMITTED 待批改 / REVIEWED 已批改 */
    private String status;

    /** 分数，0 到 100 的整数，批改后才有 */
    private Integer score;

    /** 评语，可空，不超过 500 字 */
    private String comment;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 批改时间，批改后才有 */
    private LocalDateTime reviewedAt;
}
