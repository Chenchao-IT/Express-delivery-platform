package com.campus.express.dto.packagecenter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 包裹合并验证结果（文档 1.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeValidationResult {
    private boolean eligible;
    private List<String> violations;
    private boolean recommended;
    private List<LiabilitySegmentDto> liabilitySegments;
    private List<OperationConstraintDto> operationConstraints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiabilitySegmentDto {
        private Long packageId;
        private String trackingNumber;
        private String carrier;
        private String station;
        private LiabilityOwnerDto liabilityOwner;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiabilityOwnerDto {
        private String type;
        private String id;
        private String name;
        private String contact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationConstraintDto {
        private String type;
        private String description;
    }
}
