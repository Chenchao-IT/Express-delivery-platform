package com.campus.express.service;

import com.campus.express.dto.packagecenter.*;
import com.campus.express.entity.Package;
import com.campus.express.entity.VirtualShelf;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.VirtualShelfRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 包裹聚合服务 - 明确业务边界（文档 1.1）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PackageAggregationService {

    private final PackageRepository packageRepository;
    private final VirtualShelfRepository shelfRepository;

    private static final Map<String, Object> MERGE_LOCKS = new ConcurrentHashMap<>();

    /**
     * 检查包裹合并的合法性
     */
    public MergeValidationResult validateMergeEligibility(List<Long> packageIds) {
        if (packageIds == null || packageIds.size() < 2) {
            return MergeValidationResult.builder()
                .eligible(false)
                .violations(List.of("至少需要选择2个包裹才能合并"))
                .recommended(false)
                .liabilitySegments(List.of())
                .operationConstraints(List.of())
                .build();
        }

        List<Package> packages = packageRepository.findAllById(packageIds);
        if (packages.size() != packageIds.size()) {
            return MergeValidationResult.builder()
                .eligible(false)
                .violations(List.of("部分包裹不存在"))
                .recommended(false)
                .liabilitySegments(List.of())
                .operationConstraints(List.of())
                .build();
        }

        List<String> violations = new ArrayList<>();
        boolean mandatoryOk = checkMandatoryRules(packages, violations);

        return MergeValidationResult.builder()
            .eligible(mandatoryOk)
            .violations(violations)
            .recommended(checkRecommendedRules(packages))
            .liabilitySegments(defineLiabilitySegments(packages))
            .operationConstraints(defineOperationConstraints(packages))
            .build();
    }

    private boolean checkMandatoryRules(List<Package> packages, List<String> violations) {
        Set<Long> studentIds = packages.stream().map(Package::getStudentId).collect(Collectors.toSet());
        if (studentIds.size() > 1) {
            violations.add("包裹必须属于同一收件人");
            return false;
        }

        Set<String> statuses = packages.stream()
            .map(p -> p.getStatus().name())
            .collect(Collectors.toSet());
        if (!statuses.equals(Set.of("IN_STORAGE"))) {
            violations.add("包裹必须同为待取件状态");
            return false;
        }

        Set<String> zones = new HashSet<>();
        for (Package pkg : packages) {
            String zone = getShelfZone(pkg.getShelfCode());
            zones.add(zone);
        }
        if (zones.size() > 1) {
            violations.add("包裹必须位于同一货架区域");
            return false;
        }

        return true;
    }

    private boolean checkRecommendedRules(List<Package> packages) {
        return packages.size() >= 2;
    }

    private String getShelfZone(String shelfCode) {
        if (shelfCode == null || shelfCode.isEmpty()) return "DEFAULT";
        Optional<VirtualShelf> shelf = shelfRepository.findByShelfCode(shelfCode);
        return shelf.map(VirtualShelf::getZone).orElse(shelfCode.split("-")[0]);
    }

    private List<MergeValidationResult.LiabilitySegmentDto> defineLiabilitySegments(List<Package> packages) {
        return packages.stream().map(pkg -> {
            MergeValidationResult.LiabilityOwnerDto owner = MergeValidationResult.LiabilityOwnerDto.builder()
                .type("STATION")
                .id("station-1")
                .name("校园驿站")
                .contact("400-xxx-xxxx")
                .build();
            return MergeValidationResult.LiabilitySegmentDto.builder()
                .packageId(pkg.getId())
                .trackingNumber(pkg.getTrackingNumber())
                .carrier("校园快递")
                .station(getShelfZone(pkg.getShelfCode()))
                .liabilityOwner(owner)
                .build();
        }).collect(Collectors.toList());
    }

    private List<MergeValidationResult.OperationConstraintDto> defineOperationConstraints(List<Package> packages) {
        return List.of(
            MergeValidationResult.OperationConstraintDto.builder()
                .type("BATCH_PICKUP")
                .description("请一次性出示所有取件码")
                .build()
        );
    }

    /**
     * 合并取件操作
     */
    @Transactional
    public MergedPickupResult executeMergedPickup(List<Long> packageIds, String username) {
        String transactionId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String lockKey = "merged_pickup:" + username;

        synchronized (MERGE_LOCKS.computeIfAbsent(lockKey, k -> new Object())) {
            try {
                MergeValidationResult validation = validateMergeEligibility(packageIds);
                if (!validation.isEligible()) {
                    return MergedPickupResult.builder()
                        .success(false)
                        .transactionId(transactionId)
                        .error("包裹不可合并: " + String.join(", ", validation.getViolations()))
                        .fallbackAction(getFallbackAction(packageIds))
                        .build();
                }

                List<String> pickupCodes = new ArrayList<>();
                List<String> instructions = new ArrayList<>();
                instructions.add("请前往驿站，依次出示以下取件码");

                for (Package pkg : packageRepository.findAllById(packageIds)) {
                    String code = generatePickupCode(pkg);
                    pickupCodes.add(code);
                    instructions.add(String.format("包裹 %s 取件码: %s", pkg.getTrackingNumber(), code));
                    pkg.setStatus(Package.PackageStatus.PICKED_UP);
                    packageRepository.save(pkg);
                }

                return MergedPickupResult.builder()
                    .success(true)
                    .transactionId(transactionId)
                    .pickupCodes(pickupCodes)
                    .instructions(instructions)
                    .liabilities(validation.getLiabilitySegments())
                    .estimatedTimeMinutes(5)
                    .build();
            } catch (Exception e) {
                log.error("合并取件失败: {}", e.getMessage());
                return MergedPickupResult.builder()
                    .success(false)
                    .transactionId(transactionId)
                    .error(e.getMessage())
                    .fallbackAction(getFallbackAction(packageIds))
                    .build();
            } finally {
                MERGE_LOCKS.remove(lockKey);
            }
        }
    }

    private String generatePickupCode(Package pkg) {
        return String.format("%04d", Math.abs(pkg.getId().hashCode() % 10000));
    }

    private MergedPickupResult.FallbackActionDto getFallbackAction(List<Long> packageIds) {
        List<MergedPickupResult.SequentialStepDto> steps = new ArrayList<>();
        for (int i = 0; i < packageIds.size(); i++) {
            steps.add(MergedPickupResult.SequentialStepDto.builder()
                .packageId(packageIds.get(i))
                .step(i + 1)
                .recommendedTimeWindow("立即")
                .build());
        }
        return MergedPickupResult.FallbackActionDto.builder()
            .type("SEQUENTIAL_PICKUP")
            .description("由于系统限制，建议您按顺序取件")
            .steps(steps)
            .totalEstimatedTimeMinutes(packageIds.size() * 3)
            .build();
    }
}
