package top.zsj.redis.common;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 读写封装
 * <p>
 * 把 RedisTemplate 的样板代码收在一处，业务层只关心「存什么、存多久」。
 * 三个方法值得留意：
 * <ul>
 *   <li>{@link #set(String, Object, long)} 用一条 SET key value EX 下发值和过期时间，
 *       拆成 set + expire 两条命令的话，中间进程挂掉会留下一个永不过期的 key。</li>
 *   <li>{@link #setIfAbsent(String, Object, long)} 对应 SET NX EX，用来做「不存在才写入」的间隔锁，
 *       比先 hasKey 再 set 少一个竞态窗口。</li>
 *   <li>{@link #increment(String, long)} 在第一次自增时设置过期时间，用来做固定窗口计数。</li>
 * </ul>
 *
 * @author zsj
 * @date 2026/10/19
 * @description Redis 读写封装
 */
@Component
public class RedisCache {

    /** 默认过期时长 24 小时（秒） */
    public static final long DEFAULT_EXPIRE = 60 * 60 * 24L;

    /** 不过期 */
    public static final long NOT_EXPIRE = -1L;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long expire) {
        if (expire == NOT_EXPIRE) {
            redisTemplate.opsForValue().set(key, value);
        } else {
            redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        }
    }

    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 不存在才写入，返回是否写入成功。对应 Redis 的 SET key value NX EX expire
     */
    public Boolean setIfAbsent(String key, Object value, long expire) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, expire, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增，并在第一次自增时设置过期时间
     *
     * @param key    计数 key
     * @param expire 过期秒数
     * @return 自增后的值
     */
    public Long increment(String key, long expire) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L && expire != NOT_EXPIRE) {
            expire(key, expire);
        }
        return value;
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    public void hMSet(String key, Map<String, Object> map, long expire) {
        redisTemplate.opsForHash().putAll(key, map);
        if (expire != NOT_EXPIRE) {
            expire(key, expire);
        }
    }

    public void hSet(String key, String field, Object value, long expire) {
        redisTemplate.opsForHash().put(key, field, value);
        if (expire != NOT_EXPIRE) {
            expire(key, expire);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> hGetAll(String key) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        return hashOperations.entries(key);
    }

    public void expire(String key, long expire) {
        redisTemplate.expire(key, expire, TimeUnit.SECONDS);
    }
}
