package com.campus.express.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 文档 4.2：登录限流 - Redis 实现，多级时间窗口 3/1min + 10/10min，多实例共享
 */
@Component
@ConditionalOnExpression("'${redis.host:}'.trim().length() > 0")
@Primary
public class RedisLoginRateLimiter implements LoginRateLimiterService {

    private static final String KEY_PREFIX = "login:limit:";
    private static final int WINDOW_60_SECONDS = 60;
    private static final int WINDOW_600_SECONDS = 600;
    private static final int MAX_60 = 3;
    private static final int MAX_600 = 10;

    private final RedisTemplate<String, String> stringRedisTemplate;

    public RedisLoginRateLimiter(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquire(String key) {
        String k60 = KEY_PREFIX + key + ":60";
        String k600 = KEY_PREFIX + key + ":600";
        Long n60 = stringRedisTemplate.opsForValue().increment(k60);
        if (n60 == null) n60 = 0L;
        if (n60 == 1) {
            stringRedisTemplate.expire(k60, WINDOW_60_SECONDS, TimeUnit.SECONDS);
        }
        Long n600 = stringRedisTemplate.opsForValue().increment(k600);
        if (n600 == null) n600 = 0L;
        if (n600 == 1) {
            stringRedisTemplate.expire(k600, WINDOW_600_SECONDS, TimeUnit.SECONDS);
        }
        return n60 <= MAX_60 && n600 <= MAX_600;
    }

    @Override
    public int getRemainingWaitSeconds(String key) {
        Long ttl60 = stringRedisTemplate.getExpire(KEY_PREFIX + key + ":60", TimeUnit.SECONDS);
        Long ttl600 = stringRedisTemplate.getExpire(KEY_PREFIX + key + ":600", TimeUnit.SECONDS);
        long a = ttl60 != null && ttl60 > 0 ? ttl60 : 0;
        long b = ttl600 != null && ttl600 > 0 ? ttl600 : 0;
        return (int) Math.max(a, b);
    }

    @Override
    public void clearAll() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
