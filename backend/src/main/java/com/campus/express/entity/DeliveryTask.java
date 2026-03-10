package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_task", indexes = {
    @Index(name = "idx_courier_status", columnList = "courier_id, status"),
    @Index(name = "idx_priority", columnList = "priority, created_at")
})
@Data
public class DeliveryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(nullable = false, length = 50)
    private String destination;

    @Column(name = "path_json", columnDefinition = "TEXT")
    private String pathJson;

    @Column(name = "estimated_distance", precision = 8, scale = 2)
    private BigDecimal estimatedDistance;

    @Column(name = "estimated_time", precision = 8, scale = 2)
    private BigDecimal estimatedTime;

    @Column(name = "actual_distance", precision = 8, scale = 2)
    private BigDecimal actualDistance;

    @Column(name = "actual_time", precision = 8, scale = 2)
    private BigDecimal actualTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer priority = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TaskStatus {
        PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, FAILED
    }
}
