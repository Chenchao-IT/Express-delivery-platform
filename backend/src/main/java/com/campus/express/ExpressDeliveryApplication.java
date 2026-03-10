package com.campus.express;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排除 Redisson 自动配置，统一使用 Spring Data Redis (Lettuce) 连接 Redis，
 * 避免 Redisson 在启动时连接 localhost:6379 导致无 Redis 时启动失败。
 * 无 Redis 时请使用 profile：--spring.profiles.active=no-redis
 */
@SpringBootApplication(exclude = {
    org.redisson.spring.starter.RedissonAutoConfigurationV2.class
})
@EnableScheduling
public class ExpressDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpressDeliveryApplication.class, args);
    }
}
