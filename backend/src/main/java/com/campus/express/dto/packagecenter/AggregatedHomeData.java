package com.campus.express.dto.packagecenter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 包裹中心首页聚合数据（BFF 文档 2.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedHomeData {
    private List<PackageSummaryDto> packages;
    private PackageStatsDto stats;
    private List<RecommendationDto> recommendations;
    private List<NotificationDto> notifications;
    private AggregationMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageSummaryDto {
        private Long id;
        private String trackingNumber;
        private String status;
        private String shelfCode;
        private String zone;
        private String size;
        private String pickupCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageStatsDto {
        private int incomingCount;
        private int deliveringCount;
        private int deliveredCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationDto {
        private String type;
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationDto {
        private String id;
        private String type;
        private String message;
        private String packageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationMetadata {
        private String requestId;
        private long serverTime;
        private long aggregationTime;
        private List<String> degradedServices;
        private String cacheStatus;
        private Boolean degraded;
        private String message;
    }
}
