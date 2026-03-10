package com.campus.express.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 文档 4.1：滑动验证通过后获得的 verifyToken，可选 */
    private String captchaToken;

    /** 文档 4.1/4.2：设备指纹/设备 ID，可选，用于设备维度限流 */
    private String deviceId;
}
