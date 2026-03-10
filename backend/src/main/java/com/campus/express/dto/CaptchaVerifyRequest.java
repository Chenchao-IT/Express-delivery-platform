package com.campus.express.dto;

import lombok.Data;

/**
 * 文档 4.1：验证码校验请求
 */
@Data
public class CaptchaVerifyRequest {

    private String challengeId;
    /** 用户拖到的 X 位置 */
    private Integer dragX;
}
