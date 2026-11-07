package top.zsj.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 六个阶段的演示接口
 * <p>
 * 这些接口本身没有任何鉴权代码——所有规则都在 SecurityConfig 的六条链上。
 * 这正是 Spring Security 的用法：业务代码不认识"权限"两个字。
 *
 * @author zsj
 */
@RestController
public class StageController {

    /** ① 全拦截 */
    @GetMapping("/stage1/hello")
    public Result<Map<String, Object>> stage1(Authentication authentication) {
        return Result.ok(payload("stage1 全拦截", authentication));
    }

    /** ② 白名单：公开路径 */
    @GetMapping("/stage2/public/notice")
    public Result<String> stage2Public() {
        return Result.ok("stage2 公开公告，匿名可读");
    }

    /** ② 白名单：GET 放行 */
    @GetMapping("/stage2/articles/{id}")
    public Result<String> stage2Read(@PathVariable long id) {
        return Result.ok("读到文章 " + id + "（GET 放行）");
    }

    /** ② 白名单：同一个路径，DELETE 要登录 */
    @DeleteMapping("/stage2/articles/{id}")
    public Result<String> stage2Delete(@PathVariable long id, Authentication authentication) {
        return Result.ok("删除了文章 " + id + "，操作人 " + authentication.getName());
    }

    /** ③ 角色：hasRole("ADMIN") */
    @GetMapping("/stage3/admin/users")
    public Result<Map<String, Object>> stage3Admin(Authentication authentication) {
        return Result.ok(payload("stage3 管理员列表", authentication));
    }

    /** ③ 角色：hasAnyRole("ADMIN","AUDITOR") */
    @GetMapping("/stage3/ops/metrics")
    public Result<Map<String, Object>> stage3Ops(Authentication authentication) {
        return Result.ok(payload("stage3 运维指标", authentication));
    }

    /** ③ 角色：hasAllRoles("ADMIN","AUDITOR")，少一个都不行 */
    @GetMapping("/stage3/audit/logs")
    public Result<Map<String, Object>> stage3Audit(Authentication authentication) {
        return Result.ok(payload("stage3 审计日志", authentication));
    }

    /** ③ 权限点：hasAuthority("ARTICLE_DELETE") */
    @DeleteMapping("/stage3/articles/{id}")
    public Result<String> stage3Delete(@PathVariable long id, Authentication authentication) {
        return Result.ok("stage3 删除了文章 " + id + "，操作人 " + authentication.getName());
    }

    /** ⑤ 练习 1 新增的链：登录即可 */
    @GetMapping("/stage5/hello")
    public Result<Map<String, Object>> stage5(Authentication authentication) {
        return Result.ok(payload("stage5 JWT 链", authentication));
    }

    /** ⑤ 练习 1：/stage5/admin/** 要 ADMIN */
    @GetMapping("/stage5/admin/dashboard")
    public Result<Map<String, Object>> stage5Admin(Authentication authentication) {
        return Result.ok(payload("stage5 管理面板", authentication));
    }

    private Map<String, Object> payload(String stage, Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", stage);
        data.put("username", authentication.getName());
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        data.put("authorities", authorities);
        return data;
    }
}
