package top.zsj.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;
import top.zsj.security.dto.UserCreateRequest;
import top.zsj.security.security.SecurityUserDetails;
import top.zsj.security.service.UserService;
import top.zsj.security.vo.UserVO;

import java.util.List;

/**
 * 账号管理接口（管理员）
 * <p>
 * 类上一条 @PreAuthorize("hasRole('ADMIN')") 管住整组：
 * 这一组接口只给一个角色。
 * 新增再叠一层权限点 user:write——同一组接口里不同动作门槛不同。
 * URL 规则 /admin/** 已经在 SecurityConfig 里挡过一次，三层是叠加关系，不是冗余。
 *
 * @author zsj
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /** FR-11 账号列表：返回全部账号、昵称、角色、启用状态，密码不出 service */
    @GetMapping
    public Result<List<UserVO>> list() {
        return Result.ok(userService.list());
    }

    /** FR-12 新增账号：权限点 user:write，账号与角色关联写两张表 */
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /** FR-13 删除账号：先删关联再删账号，删自己返回 30005 */
    @DeleteMapping("/{username}")
    public Result<Void> delete(@PathVariable String username,
                               @AuthenticationPrincipal SecurityUserDetails principal) {
        userService.delete(username, principal.getAccount().getUsername());
        return Result.ok();
    }
}
