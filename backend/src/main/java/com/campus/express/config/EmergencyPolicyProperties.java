package com.campus.express.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档 3.2：紧急通行证策略配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "emergency")
public class EmergencyPolicyProperties {

    private boolean enabled = false;
    private String mode = "readonly";
    private DbHealth dbHealth = new DbHealth();
    private List<Policy> policies = new ArrayList<>();
    private JwtValidation jwtValidation = new JwtValidation();
    private LocalWrites localWrites = new LocalWrites();

    @Data
    public static class DbHealth {
        private long checkInterval = 5000;
        private int failureThreshold = 3;
        private int recoveryThreshold = 10;
    }

    @Data
    public static class Policy {
        private String path;
        private List<String> methods = List.of("GET");
        private boolean allowedInEmergency = true;
        private List<String> requiredClaims = new ArrayList<>();
        private String permission;
        private boolean allowedForAll = false;
    }

    @Data
    public static class JwtValidation {
        private boolean requireSignature = true;
        private boolean requireExpiry = true;
        private boolean requireIssuer = false;
        private boolean requireAudience = false;
    }

    @Data
    public static class LocalWrites {
        private boolean enabled = true;
        private boolean syncOnRecovery = true;
        private int maxPending = 1000;
    }
}
