package com.campus.express.service;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 文档 5.1/5.2：SLA 监控服务 - 加载 sla-monitoring.yml 并对 auth.login 做 P99 合规检查
 */
@Slf4j
@Service
public class SlaMonitoringService {

    private final Timer authLoginTimer;
    private final boolean enabled;

    /** 启动时从 sla-monitoring.yml 加载，默认 150ms */
    private double p99ThresholdSeconds = 0.15;

    public SlaMonitoringService(
            Timer authLoginTimer,
            @Value("${sla.monitoring.enabled:true}") boolean enabled) {
        this.authLoginTimer = authLoginTimer;
        this.enabled = enabled;
    }

    @PostConstruct
    public void loadSlaConfig() {
        Number ms = getNestedValue("sla", "response_time", "p99", "auth_login");
        if (ms != null) {
            p99ThresholdSeconds = ms.doubleValue() / 1000.0;
            log.debug("SLA auth.login P99 阈值: {}ms", ms.longValue());
        }
    }

    /** 文档 5.2：每分钟检查 auth.login P99 是否超过 sla-monitoring.yml 中阈值 */
    @Scheduled(fixedRateString = "${sla.monitoring.check-interval:60000}")
    public void checkSlaCompliance() {
        if (!enabled) {
            return;
        }
        try {
            HistogramSnapshot snapshot = authLoginTimer.takeSnapshot();
            if (snapshot.count() == 0) {
                return;
            }
            double p99Seconds = Arrays.stream(snapshot.percentileValues())
                .filter(p -> p.percentile() == 0.99)
                .mapToDouble(p -> p.value(TimeUnit.SECONDS))
                .findFirst()
                .orElse(0);

            if (p99Seconds > p99ThresholdSeconds) {
                log.warn("[SLA] auth.login P99 超标: 当前 {}ms, 阈值 {}ms",
                    (long) (p99Seconds * 1000), (long) (p99ThresholdSeconds * 1000));
            }
        } catch (Exception e) {
            log.debug("SLA 合规检查跳过或失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Number getNestedValue(String... path) {
        try (InputStream in = new ClassPathResource("sla-monitoring.yml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) return null;
            Object current = root;
            for (int i = 0; i < path.length; i++) {
                if (!(current instanceof Map)) return null;
                current = ((Map<String, Object>) current).get(path[i]);
                if (current == null) return null;
                if (i == path.length - 1 && current instanceof Number) {
                    return (Number) current;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
