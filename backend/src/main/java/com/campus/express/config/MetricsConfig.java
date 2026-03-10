package com.campus.express.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文档 5.2 / 6.3：登录接口 Timer 发布 P99 直方图，供 Prometheus histogram_quantile 告警
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Timer authLoginTimer(MeterRegistry meterRegistry) {
        return Timer.builder("auth.login")
            .description("登录接口耗时")
            .publishPercentileHistogram()
            .publishPercentiles(0.95, 0.99)
            .register(meterRegistry);
    }
}
