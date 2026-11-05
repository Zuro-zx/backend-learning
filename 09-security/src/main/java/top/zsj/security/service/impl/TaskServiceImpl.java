package top.zsj.security.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.zsj.security.common.BusinessException;
import top.zsj.security.common.ErrorCode;
import top.zsj.security.dto.ReviewRequest;
import top.zsj.security.dto.TaskSubmitRequest;
import top.zsj.security.entity.SecTask;
import top.zsj.security.mapper.SecTaskMapper;
import top.zsj.security.model.Account;
import top.zsj.security.model.TaskDetail;
import top.zsj.security.model.TaskStatus;
import top.zsj.security.service.TaskService;
import top.zsj.security.vo.TaskVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务服务实现
 * <p>
 * 四种鉴权粒度里的第四种在这里：数据归属判断。
 * /api/tasks/{id} 对登录用户开放，注解查不出问题——学生确实有 task:read，
 * 真正拦住他的是 detail() 里那句 owner 比对。只写注解不写归属判断，
 * 改一下 URL 里的 id 就能看别人的作业。
 * <p>
 * 业务规则：BR-1 归属只从令牌取、BR-3 待批改不能重复提交、
 * BR-4 已批改不能再批改。
 *
 * @author zsj
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final SecTaskMapper taskMapper;

    @Override
    public TaskVO submit(Account me, TaskSubmitRequest request) {
        // BR-3：同一学生同标题任务仍是待批改时，第二次提交返回 30011
        Long pending = taskMapper.selectCount(Wrappers.<SecTask>lambdaQuery()
                .eq(SecTask::getOwnerId, me.getId())
                .eq(SecTask::getTitle, request.title())
                .eq(SecTask::getStatus, TaskStatus.SUBMITTED.name()));
        if (pending != null && pending > 0) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_SUBMITTED);
        }

        SecTask task = new SecTask();
        // BR-1：提交人取自令牌，请求体里写 owner 不生效
        task.setOwnerId(me.getId());
        task.setTitle(request.title());
        task.setContent(request.content());
        task.setStatus(TaskStatus.SUBMITTED.name());
        task.setSubmittedAt(LocalDateTime.now());
        taskMapper.insert(task);

        return toVO(taskMapper.selectDetail(task.getId()));
    }

    @Override
    public List<TaskVO> mine(Account me) {
        return taskMapper.selectByOwner(me.getId()).stream().map(TaskServiceImpl::toVO).toList();
    }

    @Override
    public List<TaskVO> listAll() {
        return taskMapper.selectAll().stream().map(TaskServiceImpl::toVO).toList();
    }

    @Override
    public TaskVO detail(Long id, Account me) {
        TaskDetail detail = taskMapper.selectDetail(id);
        if (detail == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        boolean owner = detail.getOwnerId().equals(me.getId());
        boolean reviewer = me.hasAuthority("task:review");
        if (!owner && !reviewer) {
            // 留一条越权告警，验收 AC-5 要求日志里能看到
            log.warn("越权告警：用户 {}（id={}）试图查看任务 {}（归属用户 id={}）",
                    me.getUsername(), me.getId(), id, detail.getOwnerId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toVO(detail);
    }

    @Override
    public TaskVO review(Long id, Account me, ReviewRequest request) {
        SecTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        // BR-4：分数一旦给学生看就是结论，不能再改
        if (TaskStatus.REVIEWED.name().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_REVIEWED);
        }
        task.setScore(request.score());
        task.setComment(request.comment());
        task.setStatus(TaskStatus.REVIEWED.name());
        task.setReviewedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("教师 {} 批改任务 {}，分数 {}", me.getUsername(), id, request.score());
        return toVO(taskMapper.selectDetail(id));
    }

    /** 投影转视图：状态码补上中文文案 */
    private static TaskVO toVO(TaskDetail detail) {
        TaskStatus status = TaskStatus.fromCode(detail.getStatus());
        return TaskVO.builder()
                .id(detail.getId())
                .title(detail.getTitle())
                .content(detail.getContent())
                .ownerId(detail.getOwnerId())
                .owner(detail.getOwner())
                .ownerNickname(detail.getOwnerNickname())
                .status(detail.getStatus())
                .statusText(status == null ? detail.getStatus() : status.getText())
                .score(detail.getScore())
                .comment(detail.getComment())
                .submittedAt(detail.getSubmittedAt())
                .reviewedAt(detail.getReviewedAt())
                .build();
    }
}
