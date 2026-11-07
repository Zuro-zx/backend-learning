package top.zsj.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.security.common.Result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单登录的两个辅助接口
 * <p>
 * 登录页是纯静态 HTML，服务端渲染不了 token，所以单独给一个取 token 的接口：
 * 页面加载时 fetch 一次，把 token 填进隐藏域。这也是"静态页 + CSRF"的常规做法。
 *
 * @author zsj
 */
@RestController
public class Stage4Controller {

    /** 取 CSRF token：访问它会让 CookieCsrfTokenRepository 真正生成并下发 XSRF-TOKEN */
    @GetMapping("/stage4/csrf")
    public Result<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("headerName", token.getHeaderName());
        data.put("parameterName", token.getParameterName());
        data.put("token", token.getToken());
        return Result.ok(data);
    }

    /** 登录成功后的落地页：证明 Session 身份已经建立 */
    @GetMapping("/stage4/me")
    public String me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"><title>stage4 · 已登录</title>
                <style>
                  body{font-family:system-ui,sans-serif;background:#0f1115;color:#e6e8eb;
                       display:flex;justify-content:center;padding-top:80px}
                  .card{background:#181b21;border:1px solid #2a2f3a;border-radius:12px;padding:28px 36px}
                  code{color:#7ee787}
                </style></head>
                <body><div class="card">
                  <h2>表单登录成功</h2>
                  <p>当前用户：<code>%s</code></p>
                  <p>权限：<code>%s</code></p>
                  <p><a href="/stage4/logout" style="color:#58a6ff">退出登录</a></p>
                </div></body></html>
                """.formatted(authentication.getName(), String.join(", ", authorities));
    }
}
