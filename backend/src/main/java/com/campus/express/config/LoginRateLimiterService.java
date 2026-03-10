package com.campus.express.config;

/**
 * 文档 4.2：登录限流接口，支持内存/Redis 两种实现
 */
public interface LoginRateLimiterService {

    boolean tryAcquire(String key);

    int getRemainingWaitSeconds(String key);

    /** 清空所有限流记录（调试/管理员用） */
    void clearAll();
}
