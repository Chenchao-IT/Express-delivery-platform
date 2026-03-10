package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档 2.2：申诉审核结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {

    private boolean approved;
    private Long appealId;
    private String comments;

    public static ReviewResult approved(Long appealId) {
        return ReviewResult.builder().approved(true).appealId(appealId).build();
    }

    public static ReviewResult rejected(Long appealId, String comments) {
        return ReviewResult.builder().approved(false).appealId(appealId).comments(comments).build();
    }
}
