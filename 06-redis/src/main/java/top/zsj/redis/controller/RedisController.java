package top.zsj.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zsj
 * @date 2026/9/23
 * @description RedisController
 */
@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 读取redis string
     */
    @GetMapping("/string")
    public Map<String, Object> getString(@RequestParam String key){
        String value = stringRedisTemplate.opsForValue().get(key);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("key",key);
        result.put("value",value);
        return result;
    }
    @GetMapping("/hello")
    public String hello() {
        return "缓存与 Redis 模块";
    }
    
    /**
     * String：最基础的读写。向 Redis 写入数据，可给定默认失效时间（秒），不传默认 60 秒
     */
    @PostMapping("/string")
    public Map<String, Object> setString(@RequestParam String key,
                                         @RequestParam String value,
                                         @RequestParam(defaultValue = "60") Long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("value", value);
        // 返回 Redis 中该 key 的真实剩余过期时间（秒），而不是回显入参
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key, TimeUnit.SECONDS));
        return result;
    }
}
