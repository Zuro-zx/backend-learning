package top.zsj.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;
import top.zsj.security.service.ArticleService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务接口：鉴权规则写在过滤器链上，方法级权限写在 Service 上
 *
 * @author zsj
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ArticleService articleService;

    public ApiController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 登录即可访问：用来验证认证是否生效 */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello(Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "你好，" + authentication.getName());
        data.put("authorities", authoritiesOf(authentication));
        return Result.ok(data);
    }

    /** 管理员接口：hasRole("ADMIN") */
    @GetMapping("/admin/dashboard")
    public Result<Map<String, Object>> dashboard(Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "管理员面板：" + authentication.getName());
        data.put("authorities", authoritiesOf(authentication));
        return Result.ok(data);
    }

    /** 公开接口：permitAll */
    @GetMapping("/public/notice")
    public Result<String> notice() {
        return Result.ok("这是公开公告，不需要登录");
    }

    /** 删除文章：需要 ARTICLE_DELETE 权限点（alice 有，bob 没有——管理员不是万能的） */
    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable long id) {
        return Result.ok("已删除文章 " + id);
    }

    /** 批改文章：权限写在 ArticleServiceImpl.review 上（@PreAuthorize hasRole('AUDITOR')） */
    @PostMapping("/articles/{id}/review")
    public Result<String> review(@PathVariable long id) {
        articleService.review(id);
        return Result.ok("已批改文章 " + id);
    }

    /** 练习 4 的反例：同类内部调用，@PreAuthorize 不生效 */
    @PostMapping("/articles/self-invoke-review")
    public Result<String> selfInvokeReview() {
        return Result.ok(articleService.selfInvokeReview(0));
    }

    private List<String> authoritiesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
