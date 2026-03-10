package com.campus.express.service;

import com.campus.express.dto.packagecenter.AchievementProgressDto;
import com.campus.express.entity.Package;
import com.campus.express.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 正向成就系统 - 避免负向激励（文档 1.3）
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final PackageRepository packageRepository;

    public List<AchievementProgressDto> getAchievementProgress(Long userId) {
        List<AchievementProgressDto> result = new ArrayList<>();

        long pickedUpCount = packageRepository.findByStudentId(userId).stream()
            .filter(p -> p.getStatus() == Package.PackageStatus.PICKED_UP)
            .count();

        result.add(AchievementProgressDto.builder()
            .achievementId("first_pickup")
            .title("首次取件")
            .condition("完成首次取件")
            .progress(pickedUpCount >= 1 ? 1.0 : 0.0)
            .unlocked(pickedUpCount >= 1)
            .estimatedUnlockTime(pickedUpCount >= 1 ? null : "完成一次取件即可解锁")
            .build());

        result.add(AchievementProgressDto.builder()
            .achievementId("pickup_streak")
            .title("取件达人")
            .condition("累计取件3次")
            .progress(Math.min(pickedUpCount / 3.0, 1.0))
            .unlocked(pickedUpCount >= 3)
            .estimatedUnlockTime(pickedUpCount >= 3 ? null : "再取" + (3 - pickedUpCount) + "个包裹即可")
            .build());

        result.add(AchievementProgressDto.builder()
            .achievementId("use_scheduled_delivery")
            .title("预约配送")
            .condition("使用预约配送服务")
            .progress(0.0)
            .unlocked(false)
            .estimatedUnlockTime("预约一次配送即可解锁")
            .build());

        return result;
    }
}
