package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档 2.2：申诉提交结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealResult {

    private boolean success;
    private String message;
    private Long appealId;
    private LocalDateTime slaDeadline;

    public static AppealResult denied(String message) {
        return AppealResult.builder().success(false).message(message).build();
    }

    public static AppealResult failed(String message) {
        return AppealResult.builder().success(false).message(message).build();
    }

    public static AppealResult submitted(Long appealId, LocalDateTime slaDeadline) {
        return AppealResult.builder()
            .success(true)
            .message("申诉已提交，将在30分钟内处理")
            .appealId(appealId)
            .slaDeadline(slaDeadline)
            .build();
    }
}
