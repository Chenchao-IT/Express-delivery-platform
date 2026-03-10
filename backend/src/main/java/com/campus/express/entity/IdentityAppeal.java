package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档 2.2：身份申诉记录
 */
@Entity
@Table(name = "identity_appeal", indexes = {
    @Index(name = "idx_appeal_user_id", columnList = "user_id"),
    @Index(name = "idx_appeal_shadow_id", columnList = "shadow_id"),
    @Index(name = "idx_appeal_status", columnList = "status"),
    @Index(name = "idx_appeal_sla_deadline", columnList = "sla_deadline")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shadow_id", nullable = false)
    private Long shadowId;

    @Column(name = "appeal_type", length = 30)
    private String appealType;

    /** 证明材料 URL，逗号分隔 */
    @Column(name = "proof_urls", length = 1000)
    private String proofUrls;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppealStatus status;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "sla_deadline", nullable = false)
    private LocalDateTime slaDeadline;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id", length = 50)
    private String reviewerId;

    @Column(name = "review_comments", length = 500)
    private String reviewComments;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public enum AppealStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
