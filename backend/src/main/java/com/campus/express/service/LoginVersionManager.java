package com.campus.express.service;

import com.campus.express.dto.LoginVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 文档 1.2：登录版本号管理（Redis）
 * 密码修改、封号时递增 version；校验 JWT 时比对 token 内 version 与 Redis 中版本。
 * 无 Redis 时不创建此 Bean，JWT 仅校验签名与过期。
 */
@Service
@ConditionalOnBean(name = "loginVersionRedisTemplate")
@RequiredArgsConstructor
public class LoginVersionManager {

    private static final String KEY_PREFIX = "login:version:";
    private static final int TTL_DAYS = 30;

    private final RedisTemplate<String, LoginVersion> versionTemplate;

    /**
     * 文档 1.2：递增登录版本号（密码修改、封号等）
     */
    public Integer incrementVersion(Long userId, String reason) {
        String key = KEY_PREFIX + userId;
        LoginVersion current = versionTemplate.opsForValue().get(key);
        int newVersion = (current == null) ? 1 : current.getVersion() + 1;
        LoginVersion newObj = LoginVersion.builder()
            .userId(userId)
            .version(newVersion)
            .timestamp(System.currentTimeMillis())
            .reason(reason)
            .build();
        versionTemplate.opsForValue().set(key, newObj, TTL_DAYS, TimeUnit.DAYS);
        return newVersion;
    }

    /**
     * 文档 1.2：校验 Token 版本号
     */
    public boolean validateTokenVersion(Long userId, Integer tokenVersion) {
        String key = KEY_PREFIX + userId;
        LoginVersion current = versionTemplate.opsForValue().get(key);
        if (current == null) {
            current = LoginVersion.builder()
                .userId(userId)
                .version(1)
                .timestamp(System.currentTimeMillis())
                .reason("initial")
                .build();
            versionTemplate.opsForValue().set(key, current, TTL_DAYS, TimeUnit.DAYS);
            return tokenVersion == null || (tokenVersion >= 1);
        }
        return tokenVersion != null && tokenVersion >= current.getVersion();
    }
}
