package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档 2.2：关键冲突时创建影子账户记录
 */
@Entity
@Table(name = "shadow_account", indexes = {
    @Index(name = "idx_shadow_original_username", columnList = "original_username"),
    @Index(name = "idx_shadow_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_username", nullable = false, length = 50)
    private String originalUsername;

    @Column(name = "conflict_type", length = 50)
    private String conflictType;

    /** 冲突来源，存 JSON 数组如 ["USER_PHONE","USER_EMAIL"] */
    @Column(name = "conflicting_sources", length = 500)
    private String conflictingSources;

    /** 冲突数据快照，存 JSON */
    @Column(name = "conflict_data", columnDefinition = "TEXT")
    private String conflictData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShadowStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum ShadowStatus {
        PENDING,
        RESOLVED
    }
}
