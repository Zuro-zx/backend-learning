package top.zsj.security.service;

import top.zsj.security.dto.ReviewRequest;
import top.zsj.security.dto.TaskSubmitRequest;
import top.zsj.security.model.Account;
import top.zsj.security.vo.TaskVO;

import java.util.List;

/**
 * 任务服务：提交、我的列表、全班列表、详情、批改
 *
 * @author zsj
 */
public interface TaskService {

    /** 提交任务（FR-04），提交人取自令牌，请求体里的 owner 不生效 */
    TaskVO submit(Account me, TaskSubmitRequest request);

    /** 我的提交（FR-05），只返回自己的 */
    List<TaskVO> mine(Account me);

    /** 全班提交（FR-08），按提交时间倒序 */
    List<TaskVO> listAll();

    /** 单条详情（FR-06）：本人或持有 task:review 的教师与管理员，其余 403 */
    TaskVO detail(Long id, Account me);

    /** 批改打分（FR-09） */
    TaskVO review(Long id, Account me, ReviewRequest request);
}
