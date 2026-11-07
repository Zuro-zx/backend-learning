package top.zsj.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.zsj.security.config.SecurityProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与解析
 * <p>
 * payload 只是 Base64 编码，不是加密：任何人拿到 token 都能解开看到内容，所以不要往里塞
 * 密码、手机号、身份证。签名的意义是防篡改，不是防偷看。
 *
 * @author zsj
 */
@Component
public class JwtService {

    /** 权限列表在 payload 里的键名 */
    public static final String CLAIM_AUTHORITIES = "authorities";

    /** Bearer 前缀，注意带空格：{@code "Bearer "} 与 {@code "Bearer"} 是两个不同的字符串 */
    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    private final Duration expire;

    // 两个构造器，Spring 无法自行判断用哪个，必须显式指定
    @Autowired
    public JwtService(SecurityProperties properties) {
        this(properties.getJwt().getSecret(), Duration.ofMinutes(properties.getJwt().getExpireMinutes()));
    }

    /**
     * 直接指定密钥与有效期，单元测试用
     *
     * @param secret 密钥，字节数决定算法
     * @param expire 有效期
     */
    public JwtService(String secret, Duration expire) {
        // Keys.hmacShaKeyFor 按字节数选算法：32 字节以下 HS256，48 字节 HS384，64 字节及以上 HS512；
        // 太短会直接抛 WeakKeyException，不会静默降级
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    /**
     * 签发结果：token 本体 + 本次的 jti，调用方要拿 jti 去写白名单
     *
     * @param token 三段式 JWT
     * @param jti   唯一标识
     * @param ttl   有效期
     */
    public record IssuedToken(String token, String jti, Duration ttl) {
    }

    /**
     * 签发 token
     *
     * @param username    登录名
     * @param authorities 权限列表，含 Security 7 自动追加的认证因子（如 FACTOR_PASSWORD）
     * @return 三段式 token
     */
    public String generate(String username, List<String> authorities) {
        return issue(username, authorities).token();
    }

    /**
     * 签发 token 并返回 jti
     * <p>
     * jti 是练习 2、3 的基础：主动失效与单端登录都靠它区分"哪一个 token"。
     *
     * @param username    登录名
     * @param authorities 权限列表
     * @return token 与 jti
     */
    public IssuedToken issue(String username, List<String> authorities) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(username)
                .id(jti)
                .claim(CLAIM_AUTHORITIES, authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, expire);
    }

    /**
     * 解析并验签
     * <p>
     * verifyWith 会做三件事：验签名、验有效期、验结构。任何一项不通过都抛 JwtException 的子类
     * （SignatureException / ExpiredJwtException / MalformedJwtException），调用方接住父类即可。
     *
     * @param token 不含 Bearer 前缀
     * @return 载荷
     * @throws JwtException 签名不符、已过期或结构不对
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** token 有效期（秒），登录响应里的 expiresIn 用它 */
    public long getExpiresInSeconds() {
        return expire.toSeconds();
    }

    /** token 有效期，写 Redis 白名单时当 TTL */
    public Duration getExpire() {
        return expire;
    }
}
