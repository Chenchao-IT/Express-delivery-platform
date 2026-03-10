package com.campus.express.exception;

import lombok.Getter;

import java.util.List;

/**
 * 文档 2.2：AuthResult.invalid(validation.getErrors()) 时抛出，由 GlobalExceptionHandler 返回 400 + errors
 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(errors != null && !errors.isEmpty() ? String.join("；", errors) : "参数校验失败");
        this.errors = errors != null ? errors : List.of();
    }
}
