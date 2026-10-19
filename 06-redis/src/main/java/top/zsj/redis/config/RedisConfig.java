package top.zsj.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis 序列化配置
 * <p>
 * Spring Boot 自动装配的 RedisTemplate 默认用 JdkSerializationRedisSerializer，
 * 写进去的 key/value 是带类型前缀的二进制乱码，redis-cli 里没法看、跨语言也没法共享。
 * <p>
 * 这里做两件事：
 * 1. key（含 hash 的 field）用 StringRedisSerializer，写成普通字符串；
 * 2. value 用 GenericJacksonJsonRedisSerializer，写成 JSON。
 * <p>
 * JSON 序列化器必须显式打开类型信息并给出白名单：不带类型信息，读回来只能得到
 * LinkedHashMap，强转会失败；白名单放太宽又会变成反序列化注入的入口，
 * 所以只放开 top.zsj.、java.lang.、java.util.、java.math. 四个前缀。
 *
 * @author zsj
 * @date 2026/10/19
 * @description Redis 序列化配置
 */
@Configuration
public class RedisConfig {

    private GenericJacksonJsonRedisSerializer jsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("top.zsj.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.math.")
                        .build())
                .enableSpringCacheNullValueSupport()
                // 日期写成 ISO 字符串而不是时间戳，排查时肉眼可读
                .customize(builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS))
                .build();
    }

    /**
     * key 用字符串、value 用 JSON 的 RedisTemplate
     *
     * @param connectionFactory Lettuce 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer valueSerializer = jsonSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
