package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档 4.1：验证码校验结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyResult {

    private boolean success;
    private String message;
    /** 验证通过时返回，登录时携带 */
    private String verifyToken;

    public static CaptchaVerifyResult ok(String verifyToken) {
        return CaptchaVerifyResult.builder().success(true).verifyToken(verifyToken).build();
    }

    public static CaptchaVerifyResult fail(String message) {
        return CaptchaVerifyResult.builder().success(false).message(message).build();
    }
}
