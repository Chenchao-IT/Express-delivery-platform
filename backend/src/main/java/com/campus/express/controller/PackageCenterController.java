package com.campus.express.controller;

import com.campus.express.dto.packagecenter.*;
import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.service.AchievementService;
import com.campus.express.service.PackageAggregationService;
import com.campus.express.service.PackageHomeAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 包裹中心 API（文档 1.1, 2.1, 1.3）
 */
@RestController
@RequestMapping("/package-center")
@RequiredArgsConstructor
public class PackageCenterController {

    private final PackageHomeAggregator homeAggregator;
    private final PackageAggregationService aggregationService;
    private final AchievementService achievementService;
    private final UserRepository userRepository;

    /**
     * 首页聚合数据（BFF）
     */
    @GetMapping("/home")
    public ResponseEntity<AggregatedHomeData> getHomeData(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = userRepository.findByUsername(user.getUsername())
            .map(User::getId).orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(AggregatedHomeData.builder()
                .packages(List.of())
                .stats(AggregatedHomeData.PackageStatsDto.builder()
                    .incomingCount(0).deliveringCount(0).deliveredCount(0).build())
                .recommendations(List.of())
                .notifications(List.of())
                .metadata(AggregatedHomeData.AggregationMetadata.builder()
                    .requestId("")
                    .serverTime(System.currentTimeMillis())
                    .aggregationTime(0)
                    .degradedServices(List.of())
                    .cacheStatus("none")
                    .build())
                .build());
        }
        return ResponseEntity.ok(homeAggregator.aggregateHomeData(userId));
    }

    /**
     * 验证合并取件合法性
     */
    @PostMapping("/merge/validate")
    public ResponseEntity<MergeValidationResult> validateMerge(
            @RequestBody List<Long> packageIds) {
        return ResponseEntity.ok(aggregationService.validateMergeEligibility(packageIds));
    }

    /**
     * 执行合并取件
     */
    @PostMapping("/merge/pickup")
    public ResponseEntity<MergedPickupResult> executeMergedPickup(
            @RequestBody List<Long> packageIds,
            @AuthenticationPrincipal UserDetails user) {
        MergedPickupResult result = aggregationService.executeMergedPickup(
            packageIds, user.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * 成就进度
     */
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementProgressDto>> getAchievements(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = userRepository.findByUsername(user.getUsername())
            .map(User::getId).orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(achievementService.getAchievementProgress(userId));
    }
}
