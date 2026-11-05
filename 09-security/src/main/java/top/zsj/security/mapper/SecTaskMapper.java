package top.zsj.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.zsj.security.entity.SecTask;
import top.zsj.security.model.TaskDetail;

import java.util.List;

/**
 * 任务提交 Mapper
 * <p>
 * 列表与详情要带学生姓名与账号（FR-08），单表查不出来，联表用注解 SQL
 * 返回投影对象 TaskDetail——不返回 entity，表结构变化不外泄。
 *
 * @author zsj
 */
@Mapper
@Repository
public interface SecTaskMapper extends BaseMapper<SecTask> {

    /** 单条详情（联 sec_user 拿提交人账号与昵称） */
    @Select("""
            SELECT t.id, t.owner_id, u.username AS owner, u.nickname AS owner_nickname,
                   t.title, t.content, t.status, t.score, t.comment,
                   t.submitted_at, t.reviewed_at
            FROM sec_task t
            JOIN sec_user u ON t.owner_id = u.id
            WHERE t.id = #{id}
            """)
    TaskDetail selectDetail(@Param("id") Long id);

    /** 我的提交：只查自己的，按提交时间倒序 */
    @Select("""
            SELECT t.id, t.owner_id, u.username AS owner, u.nickname AS owner_nickname,
                   t.title, t.content, t.status, t.score, t.comment,
                   t.submitted_at, t.reviewed_at
            FROM sec_task t
            JOIN sec_user u ON t.owner_id = u.id
            WHERE t.owner_id = #{ownerId}
            ORDER BY t.submitted_at DESC
            """)
    List<TaskDetail> selectByOwner(@Param("ownerId") Long ownerId);

    /** 全班提交：返回全部，按提交时间倒序，每条带学生姓名与账号（FR-08） */
    @Select("""
            SELECT t.id, t.owner_id, u.username AS owner, u.nickname AS owner_nickname,
                   t.title, t.content, t.status, t.score, t.comment,
                   t.submitted_at, t.reviewed_at
            FROM sec_task t
            JOIN sec_user u ON t.owner_id = u.id
            ORDER BY t.submitted_at DESC
            """)
    List<TaskDetail> selectAll();
}
