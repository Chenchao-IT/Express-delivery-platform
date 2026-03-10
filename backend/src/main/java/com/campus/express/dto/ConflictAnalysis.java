package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * 文档 2.2：冲突分析结果
 * - 关键冲突：唯一键冲突（同一手机/邮箱被多个账号使用）
 * - 非关键冲突：多源同用户字段不一致（如手机/邮箱/姓名）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAnalysis {

    private boolean criticalConflict;
    private boolean nonCriticalConflict;

    /** 冲突类型，如 DUPLICATE_PHONE, DUPLICATE_EMAIL, MULTI_SOURCE_FIELD_MISMATCH */
    private String conflictType;

    /** 冲突来源标识 */
    private Set<String> conflictingSources;

    /** 冲突数据快照（可选） */
    private Object conflictData;

    /** 冲突字段名，如 phone, email, realName */
    private Set<String> conflictingFields;

    public boolean hasCriticalConflict() {
        return criticalConflict;
    }

    public boolean hasNonCriticalConflict() {
        return nonCriticalConflict;
    }

    public static ConflictAnalysis noConflict() {
        return ConflictAnalysis.builder()
            .criticalConflict(false)
            .nonCriticalConflict(false)
            .conflictingSources(Collections.emptySet())
            .conflictingFields(Collections.emptySet())
            .build();
    }
}
