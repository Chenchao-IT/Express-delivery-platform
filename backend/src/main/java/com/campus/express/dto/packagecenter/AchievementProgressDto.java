package com.campus.express.dto.packagecenter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成就进度 DTO（文档 1.3）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementProgressDto {
    private String achievementId;
    private String title;
    private String condition;
    private double progress;
    private boolean unlocked;
    private String estimatedUnlockTime;
}
