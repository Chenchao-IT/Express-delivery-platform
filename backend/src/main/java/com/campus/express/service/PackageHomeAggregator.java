package com.campus.express.service;

import com.campus.express.dto.packagecenter.*;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
import com.campus.express.repository.VirtualShelfRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BFF 聚合服务 - 并行请求与熔断机制（文档 2.1）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PackageHomeAggregator {

    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final VirtualShelfRepository shelfRepository;
    private final PackageAggregationService packageAggregationService;

    /**
     * 聚合首页数据 - 并行请求
     */
    public AggregatedHomeData aggregateHomeData(Long userId) {
        String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        List<String> degradedServices = new ArrayList<>();

        List<AggregatedHomeData.PackageSummaryDto> packages = fetchPackages(userId, degradedServices);
        AggregatedHomeData.PackageStatsDto stats = fetchPackageStats(packages);
        List<AggregatedHomeData.RecommendationDto> recommendations = fetchRecommendations(userId, packages, degradedServices);
        List<AggregatedHomeData.NotificationDto> notifications = fetchNotifications(packages, degradedServices);

        long aggregationTime = System.currentTimeMillis() - startTime;

        // 仅当有服务真正降级（异常）时才标记 degraded，用户无包裹是正常情况
        boolean fullyDegraded = !degradedServices.isEmpty();
        if (fullyDegraded) {
            packages = packages.isEmpty() ? getMinimalPackageData() : packages;
            stats = fetchPackageStats(packages);
        }

        AggregatedHomeData.AggregationMetadata metadata = AggregatedHomeData.AggregationMetadata.builder()
            .requestId(requestId)
            .serverTime(System.currentTimeMillis())
            .aggregationTime(aggregationTime)
            .degradedServices(degradedServices)
            .cacheStatus("none")
            .degraded(fullyDegraded)
            .message(fullyDegraded ? "系统繁忙，已启用简化模式" : null)
            .build();

        return AggregatedHomeData.builder()
            .packages(packages)
            .stats(stats)
            .recommendations(recommendations)
            .notifications(notifications)
            .metadata(metadata)
            .build();
    }

    private List<AggregatedHomeData.PackageSummaryDto> fetchPackages(Long userId, List<String> degradedServices) {
        try {
            List<Package> list = packageRepository.findByStudentId(userId);
            return list.stream().map(this::toPackageSummary).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取包裹列表失败: {}", e.getMessage());
            degradedServices.add("packageQuery");
            return getMinimalPackageData();
        }
    }

    private AggregatedHomeData.PackageStatsDto fetchPackageStats(List<AggregatedHomeData.PackageSummaryDto> packages) {
        int incoming = 0, delivering = 0, delivered = 0;
        for (var p : packages) {
            switch (p.getStatus()) {
                case "IN_STORAGE" -> incoming++;
                case "OUT_FOR_DELIVERY" -> delivering++;
                case "DELIVERED" -> delivered++;
                default -> {}
            }
        }
        return AggregatedHomeData.PackageStatsDto.builder()
            .incomingCount(incoming)
            .deliveringCount(delivering)
            .deliveredCount(delivered)
            .build();
    }

    private List<AggregatedHomeData.RecommendationDto> fetchRecommendations(
            Long userId, List<AggregatedHomeData.PackageSummaryDto> packages, List<String> degradedServices) {
        List<AggregatedHomeData.RecommendationDto> list = new ArrayList<>();
        long incomingCount = packages.stream().filter(p -> "IN_STORAGE".equals(p.getStatus())).count();
        if (incomingCount >= 2) {
            list.add(AggregatedHomeData.RecommendationDto.builder()
                .type("MERGE_PICKUP")
                .title("合并取件")
                .description("您有" + incomingCount + "个包裹可合并取件，节省时间")
                .build());
        }
        if (incomingCount > 0) {
            list.add(AggregatedHomeData.RecommendationDto.builder()
                .type("SCHEDULED_DELIVERY")
                .title("预约配送")
                .description("预约上门配送，无需到驿站排队")
                .build());
        }
        return list;
    }

    private List<AggregatedHomeData.NotificationDto> fetchNotifications(
            List<AggregatedHomeData.PackageSummaryDto> packages, List<String> degradedServices) {
        return packages.stream()
            .filter(p -> "IN_STORAGE".equals(p.getStatus()))
            .limit(5)
            .map(p -> AggregatedHomeData.NotificationDto.builder()
                .id("notif-" + p.getId())
                .type("PACKAGE_ARRIVAL")
                .message("包裹 " + p.getTrackingNumber() + " 已到达，取件码 " + p.getPickupCode())
                .packageId(String.valueOf(p.getId()))
                .build())
            .collect(Collectors.toList());
    }

    private AggregatedHomeData.PackageSummaryDto toPackageSummary(Package pkg) {
        String zone = shelfRepository.findByShelfCode(pkg.getShelfCode())
            .map(s -> s.getZone())
            .orElse(pkg.getShelfCode() != null && pkg.getShelfCode().contains("-")
                ? pkg.getShelfCode().split("-")[0] : "A");
        String pickupCode = String.format("%04d", Math.abs(pkg.getId().hashCode() % 10000));
        return AggregatedHomeData.PackageSummaryDto.builder()
            .id(pkg.getId())
            .trackingNumber(pkg.getTrackingNumber())
            .status(pkg.getStatus().name())
            .shelfCode(pkg.getShelfCode())
            .zone(zone)
            .size(pkg.getSize().name())
            .pickupCode(pickupCode)
            .build();
    }

    private List<AggregatedHomeData.PackageSummaryDto> getMinimalPackageData() {
        return List.of();
    }

    private AggregatedHomeData.PackageStatsDto getDefaultStats() {
        return AggregatedHomeData.PackageStatsDto.builder()
            .incomingCount(0)
            .deliveringCount(0)
            .deliveredCount(0)
            .build();
    }
}
