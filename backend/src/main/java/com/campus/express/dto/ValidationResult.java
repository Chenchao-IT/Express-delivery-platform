package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 文档 2.2：登录请求校验结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;
    private List<String> errors;

    /** 与 getValid() 等价，便于业务代码使用 validation.isValid() */
    public boolean isValid() {
        return valid;
    }

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
    }

    public static ValidationResult invalid(List<String> errors) {
        return ValidationResult.builder().valid(false).errors(errors != null ? errors : Collections.emptyList()).build();
    }
}
