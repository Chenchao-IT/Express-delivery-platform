package com.campus.express.dto.packagecenter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 合并取件结果（文档 1.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergedPickupResult {
    private boolean success;
    private String transactionId;
    private List<String> pickupCodes;
    private List<String> instructions;
    private List<MergeValidationResult.LiabilitySegmentDto> liabilities;
    private Integer estimatedTimeMinutes;
    private String error;
    private FallbackActionDto fallbackAction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FallbackActionDto {
        private String type;
        private String description;
        private List<SequentialStepDto> steps;
        private Integer totalEstimatedTimeMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SequentialStepDto {
        private Long packageId;
        private int step;
        private String recommendedTimeWindow;
    }
}
