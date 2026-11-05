package top.zsj.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析
 * <p>
 * Spring Security 6/7 都不提供 JWT 签发，jjwt 是事实标准。
 * 算法显式固定 HS256（signWith 第二个参数），不依赖默认值，
 * 防止将来库升级后默认算法变化导致旧令牌校验行为漂移。
 * 令牌里放 uid、username、roles 三样；解析失败（过期、签名对不上、格式坏）抛
 * JwtException，由过滤器决定不拦，交给后面的 401 出口。
 *
 * @author zsj
 */
@Component
public class JwtTokenProvider {

    /** 载荷里的自定义 claim 名 */
    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_ROLES = "roles";

    private final SecretKey key;
    private final long expireMillis;

    public JwtTokenProvider(JwtProperties properties) {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret 必须至少 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = properties.getJwt().getExpireMinutes() * 60_000L;
    }

    /**
     * 签发令牌：subject 放登录名，uid 与 roles 放 claim
     */
    public String generate(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_UID, userId)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 令牌过期时间，登录结果里返回给前端 */
    public LocalDateTime expiresAt() {
        return Instant.ofEpochMilli(System.currentTimeMillis() + expireMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 解析结果：只暴露业务需要的三个字段，Claims 对象不出本类
     */
    public record ParsedToken(Long userId, String username, List<String> roles) {
    }

    /**
     * 解析并校验签名与有效期；任何问题都抛 JwtException
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number uid = claims.get(CLAIM_UID, Number.class);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return new ParsedToken(
                uid == null ? null : uid.longValue(),
                claims.getSubject(),
                roles == null ? List.of() : roles);
    }
}
