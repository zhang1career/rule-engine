package lab.zhang.rule.rule_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;

/**
 * Redis configuration
 *
 * @author Rongjin Zhang
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate for Long values
     * Used for rule selection cache (userId:eventId -> ruleId)
     *
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate instance
     */
    @Bean("redisTemplateStringLong")
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use Long serializer for values
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate for Object values
     * Used for flexible cache operations (Hash operations, etc.)
     *
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate instance
     */
    @Bean("redisTemplateStringObject")
    public RedisTemplate<String, Object> redisTemplateObject(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JDK serialization for values (can handle any Object)
        template.setValueSerializer(Objects.requireNonNull(template.getDefaultSerializer()));
        template.setHashValueSerializer(template.getDefaultSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate for String values
     * Used for rule content cache (ruleId -> content)
     *
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate instance
     */
    @Bean("redisTemplateStringString")
    public RedisTemplate<String, String> redisTemplateString(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use String serializer for values
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}

