package top.zsj.redis.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的签发与校验
 * <p>
 * payload 里放手机号（sub）、签发时间（iat）、过期时间（exp），整个串用服务端密钥签名，
 * 服务端不保存任何会话状态——这就是「无状态登录态」，多实例部署不用共享 session。
 * <p>
 * 密钥长度决定算法：jjwt 会按密钥字节数选用它支持的最强 HMAC 算法，
 * 32 字节走 HS256、48 字节走 HS384、64 字节走 HS512。
 *
 * @author zsj
 * @date 2026/10/19
 * @description JWT 工具类
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtUtils(@Value("${zsj.jwt.secret}") String secret,
                    @Value("${zsj.jwt.expire-seconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 签发 token
     *
     * @param phone 手机号，写进 sub
     * @return 三段式的 JWT 字符串
     */
    public String create(String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(phone)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验签名与有效期并返回载荷；签名不对、结构不对、已过期都统一转成 40100
     *
     * @param token 不带 Bearer 前缀的 token
     * @return 载荷
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }
}
