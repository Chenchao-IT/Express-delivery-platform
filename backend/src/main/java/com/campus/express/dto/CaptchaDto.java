package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档 4.1：滑动验证码（简化版 - 数字滑块位置，无图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaDto {

    private String challengeId;
    /** 目标 X 位置 0~100，前端滑块需拖到此位置附近 */
    private Integer targetX;
    private Long expiresAt;
}
