package top.zsj.security.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 版 token 白名单
 * <p>
 * key 用 {@code user:token:{username}}，value 是当前有效的 jti。多实例部署时天然共享，
 * 重启也不丢。
 *
 * @author zsj
 */
@Component
@ConditionalOnProperty(prefix = "zsj.security.jwt", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenRegistry implements TokenRegistry {

    private static final String KEY_PREFIX = "user:token:";

    private final StringRedisTemplate redis;

    public RedisTokenRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void issue(String username, String jti, Duration ttl) {
        // 直接覆盖，不需要先删：同一账号只允许一个有效 token
        redis.opsForValue().set(KEY_PREFIX + username, jti, ttl);
    }

    @Override
    public boolean isActive(String username, String jti) {
        String current = redis.opsForValue().get(KEY_PREFIX + username);
        return jti != null && jti.equals(current);
    }

    @Override
    public void revoke(String username) {
        redis.delete(KEY_PREFIX + username);
    }
}
