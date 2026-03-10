package com.campus.express.controller;

import com.campus.express.config.LoginRateLimiterService;
import com.campus.express.dto.CaptchaDto;
import io.micrometer.core.instrument.Timer;
import com.campus.express.dto.CaptchaVerifyResult;
import com.campus.express.dto.LoginRequest;
import com.campus.express.dto.LoginResponse;
import com.campus.express.dto.RegisterRequest;
import com.campus.express.service.AuthService;
import com.campus.express.service.MinimalPermissionAuthService;
import com.campus.express.service.SlideCaptchaService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MinimalPermissionAuthService minimalPermissionAuthService;
    private final LoginRateLimiterService loginRateLimiter;
    private final SlideCaptchaService slideCaptchaService;
    private final MeterRegistry meterRegistry;
    private final Timer authLoginTimer;

    /** 清空登录限流（开发/调试用，生产环境应移除或加鉴权） */
    @PostMapping("/rate-limit/clear")
    public ResponseEntity<Map<String, Object>> clearRateLimit() {
        loginRateLimiter.clearAll();
        return ResponseEntity.ok(Map.of("success", true, "message", "限流已清空"));
    }

    /** 文档 4.1：获取滑动验证码（简化版） */
    @GetMapping("/captcha")
    public ResponseEntity<CaptchaDto> getCaptcha() {
        return ResponseEntity.ok(slideCaptchaService.generateCaptcha());
    }

    /** 文档 4.1：校验滑动验证码，返回 verifyToken 供登录使用 */
    @PostMapping("/captcha/verify")
    public ResponseEntity<CaptchaVerifyResult> verifyCaptcha(@RequestBody Map<String, Object> body) {
        String challengeId = body != null && body.get("challengeId") != null ? body.get("challengeId").toString() : null;
        Integer dragX = body != null && body.get("dragX") != null ? ((Number) body.get("dragX")).intValue() : null;
        CaptchaVerifyResult result = slideCaptchaService.verify(challengeId, dragX);
        return ResponseEntity.ok(result);
    }

    /** 文档 2.2 + 四/五：登录（IP+学号双维度限流、可选验证码、SLA 计时） */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            if (request.getCaptchaToken() != null && !request.getCaptchaToken().isBlank()) {
                if (!slideCaptchaService.consumeVerifyToken(request.getCaptchaToken())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "验证码无效或已使用，请重新验证"));
                }
            }
            String clientIp = getClientIp(httpRequest);
            String userKey = "user:" + (request.getUsername() != null ? request.getUsername().trim() : "");
            String deviceKey = request.getDeviceId() != null && !request.getDeviceId().isBlank()
                ? "device:" + request.getDeviceId().trim() : null;
            if (!loginRateLimiter.tryAcquire(clientIp)) {
                int waitSec = loginRateLimiter.getRemainingWaitSeconds(clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("success", false, "message", "登录尝试过于频繁，请" + waitSec + "秒后再试"));
            }
            if (!userKey.equals("user:") && !loginRateLimiter.tryAcquire(userKey)) {
                int waitSec = loginRateLimiter.getRemainingWaitSeconds(userKey);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("success", false, "message", "该账号登录尝试过于频繁，请" + waitSec + "秒后再试"));
            }
            if (deviceKey != null && !loginRateLimiter.tryAcquire(deviceKey)) {
                int waitSec = loginRateLimiter.getRemainingWaitSeconds(deviceKey);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("success", false, "message", "该设备登录尝试过于频繁，请" + waitSec + "秒后再试"));
            }
            LoginResponse response = minimalPermissionAuthService.loginWithConflictResolution(request);
            return ResponseEntity.ok(response);
        } finally {
            sample.stop(authLoginTimer);
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "authorities", user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList()
        ));
    }
}
