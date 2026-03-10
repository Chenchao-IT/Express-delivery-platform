package com.campus.express.config;

import com.campus.express.dto.LoginVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 文档 1.2 / 3.1：Redis 配置，LoginVersion 存储。redis.host 为空或使用 profile no-redis 时不加载。
 */
@Configuration
@ConditionalOnExpression("'${redis.host:}'.trim().length() > 0")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, LoginVersion> loginVersionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, LoginVersion> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(LoginVersion.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(LoginVersion.class));
        template.afterPropertiesSet();
        return template;
    }
}
