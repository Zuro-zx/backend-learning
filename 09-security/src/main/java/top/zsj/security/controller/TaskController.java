package top.zsj.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;
import top.zsj.security.dto.ReviewRequest;
import top.zsj.security.dto.TaskSubmitRequest;
import top.zsj.security.model.Account;
import top.zsj.security.security.SecurityUserDetails;
import top.zsj.security.service.TaskService;
import top.zsj.security.vo.TaskVO;

import java.util.List;

/**
 * 任务接口：提交、我的提交、全班提交、详情、批改
 * <p>
 * 控制器只做三件事：接请求、校验入参、用注解声明谁能调。
 * 业务判断（重复提交、归属比对）全在 Service，这里不出现 if-else 业务逻辑。
 * <p>
 * 注解只认权限点，不认角色：将来多一类人，加角色配权限点即可，这里一行都不用改。
 *
 * @author zsj
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** FR-04 提交任务：权限点 task:submit，提交人取自令牌 */
    @PostMapping
    @PreAuthorize("hasAuthority('task:submit')")
    public Result<TaskVO> submit(@AuthenticationPrincipal SecurityUserDetails principal,
                                 @Valid @RequestBody TaskSubmitRequest request) {
        return Result.ok(taskService.submit(principal.getAccount(), request));
    }

    /** FR-05 我的提交：登录即可，URL 里不带用户名 */
    @GetMapping("/mine")
    public Result<List<TaskVO>> mine(@AuthenticationPrincipal SecurityUserDetails principal) {
        return Result.ok(taskService.mine(principal.getAccount()));
    }

    /** FR-08 全班提交：权限点 task:review，学生调是 403 */
    @GetMapping
    @PreAuthorize("hasAuthority('task:review')")
    public Result<List<TaskVO>> list() {
        return Result.ok(taskService.listAll());
    }

    /** FR-06 单条详情：登录即可，归属比对在 Service 里做 */
    @GetMapping("/{id}")
    public Result<TaskVO> detail(@PathVariable Long id,
                                 @AuthenticationPrincipal SecurityUserDetails principal) {
        return Result.ok(taskService.detail(id, principal.getAccount()));
    }

    /** FR-09 批改打分：权限点 task:review */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('task:review')")
    public Result<TaskVO> review(@PathVariable Long id,
                                 @AuthenticationPrincipal SecurityUserDetails principal,
                                 @Valid @RequestBody ReviewRequest request) {
        Account me = principal.getAccount();
        return Result.ok(taskService.review(id, me, request));
    }
}
