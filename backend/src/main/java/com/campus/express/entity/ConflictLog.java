package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档 2.2：非关键冲突时记录冲突日志
 */
@Entity
@Table(name = "conflict_log", indexes = {
    @Index(name = "idx_conflict_user_id", columnList = "user_id"),
    @Index(name = "idx_conflict_logged_at", columnList = "logged_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conflict_type", nullable = false, length = 30)
    private String conflictType;

    /** 冲突字段，逗号分隔如 "phone,email" */
    @Column(name = "conflicting_fields", length = 200)
    private String conflictingFields;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        if (loggedAt == null) {
            loggedAt = LocalDateTime.now();
        }
    }
}
