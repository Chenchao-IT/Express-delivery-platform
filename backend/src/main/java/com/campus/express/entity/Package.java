package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "`package`", indexes = {
    @Index(name = "idx_tracking", columnList = "tracking_number"),
    @Index(name = "idx_student_status", columnList = "student_id, status"),
    @Index(name = "idx_shelf", columnList = "shelf_code")
})
@Data
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", unique = true, nullable = false, length = 50)
    private String trackingNumber;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "courier_id")
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageSize size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status = PackageStatus.IN_STORAGE;

    @Column(name = "shelf_code", length = 20)
    private String shelfCode;

    @Column(name = "pickup_code", length = 12)
    private String pickupCode;

    @Column(name = "storage_time")
    private LocalDateTime storageTime;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;

    @Version
    private Integer version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PackageSize {
        SMALL, MEDIUM, LARGE
    }

    public enum PackageStatus {
        IN_STORAGE, OUT_FOR_DELIVERY, DELIVERED, PICKED_UP, RETURNED, COMPLETED
    }
}
