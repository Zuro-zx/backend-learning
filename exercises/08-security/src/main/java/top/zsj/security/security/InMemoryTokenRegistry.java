package top.zsj.security.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 token 白名单：不连 Redis 也能跑通链路，重启即失效、多实例不共享
 *
 * @author zsj
 */
@Component
@ConditionalOnProperty(prefix = "zsj.security.jwt", name = "store", havingValue = "memory")
public class InMemoryTokenRegistry implements TokenRegistry {

    private record Entry(String jti, Instant expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void issue(String username, String jti, Duration ttl) {
        store.put(username, new Entry(jti, Instant.now().plus(ttl)));
    }

    @Override
    public boolean isActive(String username, String jti) {
        Entry entry = store.get(username);
        if (entry == null || jti == null) {
            return false;
        }
        if (entry.expireAt().isBefore(Instant.now())) {
            store.remove(username);
            return false;
        }
        return jti.equals(entry.jti());
    }

    @Override
    public void revoke(String username) {
        store.remove(username);
    }
}
