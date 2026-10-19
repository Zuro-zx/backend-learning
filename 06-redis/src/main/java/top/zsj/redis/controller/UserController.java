package top.zsj.redis.controller;

import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.redis.common.Result;
import top.zsj.redis.config.AuthInterceptor;
import top.zsj.redis.dto.UserInfo;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 需要登录态的接口
 * <p>
 * token 由 AuthInterceptor 验签并放进请求属性，这里只管取载荷，不用自己解析请求头。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 用户信息接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final DateTimeFormatter LOGIN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 返回当前 token 对应的用户信息（前端首页用它替代读 localStorage）
     *
     * @param claims 拦截器放入的 JWT 载荷
     * @return 手机号 + 登录时间
     */
    @GetMapping("/me")
    public Result<UserInfo> me(@RequestAttribute(AuthInterceptor.ATTR_CLAIMS) Claims claims) {
        String loginTime = LOGIN_TIME_FORMATTER.format(
                claims.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()));
        return Result.ok(new UserInfo(claims.getSubject(), loginTime));
    }
}
