package com.campus.express.service;

import com.campus.express.dto.CaptchaDto;
import com.campus.express.dto.CaptchaVerifyResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档 4.1：滑动验证码服务（简化版 - 数字位置校验，2 分钟有效）
 */
@Service
public class SlideCaptchaService {

    private static final int TARGET_MAX = 80;
    private static final int TOLERANCE = 5;
    private static final long CAPTCHA_TTL_MS = 2 * 60 * 1000;
    private static final long VERIFY_TOKEN_TTL_MS = 60 * 1000;

    private final Map<String, CaptchaData> challengeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> verifyTokenStore = new ConcurrentHashMap<>();

    public CaptchaDto generateCaptcha() {
        String challengeId = UUID.randomUUID().toString();
        int targetX = (int) (Math.random() * (TARGET_MAX - 10)) + 5;
        long expiresAt = System.currentTimeMillis() + CAPTCHA_TTL_MS;
        challengeStore.put(challengeId, new CaptchaData(targetX, System.currentTimeMillis()));
        return CaptchaDto.builder()
            .challengeId(challengeId)
            .targetX(null)
            .expiresAt(expiresAt)
            .build();
    }

    public CaptchaVerifyResult verify(String challengeId, Integer dragX) {
        if (challengeId == null || dragX == null) {
            return CaptchaVerifyResult.fail("参数缺失");
        }
        CaptchaData data = challengeStore.remove(challengeId);
        if (data == null) {
            return CaptchaVerifyResult.fail("验证数据不存在或已过期");
        }
        if (System.currentTimeMillis() - data.timestamp > CAPTCHA_TTL_MS) {
            return CaptchaVerifyResult.fail("验证码已过期");
        }
        if (Math.abs(dragX - data.targetX) > TOLERANCE) {
            return CaptchaVerifyResult.fail("验证失败");
        }
        String verifyToken = UUID.randomUUID().toString();
        verifyTokenStore.put(verifyToken, System.currentTimeMillis() + VERIFY_TOKEN_TTL_MS);
        return CaptchaVerifyResult.ok(verifyToken);
    }

    public boolean consumeVerifyToken(String token) {
        if (token == null) return false;
        Long expires = verifyTokenStore.remove(token);
        if (expires == null) return false;
        return System.currentTimeMillis() < expires;
    }

    private record CaptchaData(int targetX, long timestamp) {}
}
