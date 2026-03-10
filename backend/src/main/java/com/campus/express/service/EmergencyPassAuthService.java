package com.campus.express.service;

import com.campus.express.config.EmergencyPolicyProperties;
import com.campus.express.security.JwtTokenProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档 3.2：紧急通行证认证服务 - DB 不可用时降级 JWT 校验，仅放行策略内路径
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyPassAuthService {

    private final EmergencyPolicyProperties policy;
    private final DataSource dataSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final MeterRegistry meterRegistry;
    private final LocalWriteStorage localWriteStorage;

    private final AtomicBoolean emergencyMode = new AtomicBoolean(false);
    private final AtomicInteger dbFailureCount = new AtomicInteger(0);
    private final AtomicInteger dbSuccessCount = new AtomicInteger(0);

    /** 文档 6.3：暴露紧急模式状态供 Prometheus 告警 EmergencyModeActive */
    @PostConstruct
    public void registerEmergencyModeGauge() {
        io.micrometer.core.instrument.Gauge.builder("system_emergency_mode_active", this, s -> s.isEmergencyMode() ? 1.0 : 0.0)
            .description("1=紧急模式开启，0=正常")
            .register(meterRegistry);
    }

    /**
     * 文档 3.2：数据库健康检查，连续失败触发紧急模式，连续成功恢复
     */
    @Scheduled(fixedDelayString = "${emergency.db-health.check-interval:5000}")
    public void checkDbHealth() {
        if (!policy.isEnabled()) {
            return;
        }
        boolean healthy = isDbHealthy();
        if (!healthy) {
            int failures = dbFailureCount.incrementAndGet();
            dbSuccessCount.set(0);
            log.warn("数据库健康检查失败，连续失败次数: {}", failures);
            if (failures >= policy.getDbHealth().getFailureThreshold() && !emergencyMode.get()) {
                enterEmergencyMode();
            }
        } else {
            dbFailureCount.set(0);
            if (emergencyMode.get()) {
                int successes = dbSuccessCount.incrementAndGet();
                if (successes >= policy.getDbHealth().getRecoveryThreshold()) {
                    exitEmergencyMode();
                }
            } else {
                dbSuccessCount.set(0);
            }
        }
    }

    private boolean isDbHealthy() {
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.executeQuery("SELECT 1").close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void enterEmergencyMode() {
        if (emergencyMode.compareAndSet(false, true)) {
            log.warn("进入紧急模式：数据库不可用，仅放行策略内接口");
        }
    }

    private void exitEmergencyMode() {
        if (emergencyMode.compareAndSet(true, false)) {
            log.info("退出紧急模式：数据库已恢复");
            dbSuccessCount.set(0);
            if (policy.getLocalWrites() != null && policy.getLocalWrites().isSyncOnRecovery() && localWriteStorage != null) {
                try {
                    LocalWriteStorage.SyncResult r = localWriteStorage.syncToDatabase();
                    log.info("本地写已同步: total={}, success={}, failed={}", r.getTotal(), r.getSuccessCount(), r.getFailedCount());
                } catch (Exception e) {
                    log.warn("本地写同步失败", e);
                }
            }
        }
    }

    public boolean isEmergencyMode() {
        return policy.isEnabled() && emergencyMode.get();
    }

    /**
     * 文档 3.2：紧急模式下校验请求 - 仅校验签名与过期，并检查路径与声明
     */
    public EmergencyAuthResult authenticate(HttpServletRequest request, String token) {
        if (!isEmergencyMode()) {
            return EmergencyAuthResult.normalMode();
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        EmergencyPolicyProperties.Policy matched = findMatchingPolicy(path, method);
        if (matched == null) {
            return EmergencyAuthResult.denied("紧急模式下不允许访问此接口");
        }
        if (matched.isAllowedForAll()) {
            return EmergencyAuthResult.grantedNoAuth();
        }
        if (token == null || token.isBlank()) {
            return EmergencyAuthResult.denied("缺少 Token");
        }
        try {
            Claims claims = jwtTokenProvider.getClaims(token);
            List<String> required = matched.getRequiredClaims();
            if (required != null) {
                for (String claim : required) {
                    Object val = "sub".equals(claim) ? claims.getSubject() : claims.get(claim);
                    if (val == null || val.toString().isBlank()) {
                        return EmergencyAuthResult.denied("缺少必需声明: " + claim);
                    }
                }
            }
            return EmergencyAuthResult.granted(claims.getSubject());
        } catch (ExpiredJwtException e) {
            return EmergencyAuthResult.expired("Token 已过期");
        } catch (JwtException e) {
            return EmergencyAuthResult.denied("Token 无效");
        }
    }

    private EmergencyPolicyProperties.Policy findMatchingPolicy(String path, String method) {
        for (EmergencyPolicyProperties.Policy p : policy.getPolicies()) {
            if (!p.isAllowedInEmergency()) continue;
            if (path != null && path.equals(p.getPath()) && p.getMethods().contains(method)) {
                return p;
            }
        }
        return null;
    }

    public static final class EmergencyAuthResult {
        public static final int NORMAL = 0;
        public static final int GRANTED = 1;
        public static final int GRANTED_NO_AUTH = 2;
        public static final int DENIED = 3;
        public static final int EXPIRED = 4;

        private final int status;
        private final String username;
        private final String message;

        private EmergencyAuthResult(int status, String username, String message) {
            this.status = status;
            this.username = username;
            this.message = message;
        }

        public static EmergencyAuthResult normalMode() {
            return new EmergencyAuthResult(NORMAL, null, null);
        }

        public static EmergencyAuthResult granted(String username) {
            return new EmergencyAuthResult(GRANTED, username, null);
        }

        public static EmergencyAuthResult grantedNoAuth() {
            return new EmergencyAuthResult(GRANTED_NO_AUTH, null, null);
        }

        public static EmergencyAuthResult denied(String message) {
            return new EmergencyAuthResult(DENIED, null, message);
        }

        public static EmergencyAuthResult expired(String message) {
            return new EmergencyAuthResult(EXPIRED, null, message);
        }

        public int getStatus() { return status; }
        public String getUsername() { return username; }
        public String getMessage() { return message; }
    }
}
