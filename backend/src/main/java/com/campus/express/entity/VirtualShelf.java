package com.campus.express.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_shelf_mapping", indexes = {
    @Index(name = "idx_zone_row", columnList = "zone, row_num"),
    @Index(name = "idx_grid", columnList = "grid_x, grid_y"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
public class VirtualShelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shelf_code", unique = true, nullable = false, length = 20)
    private String shelfCode;

    @Column(nullable = false, length = 1)
    private String zone;

    @Column(name = "row_num", nullable = false)
    private Integer rowNum;

    @Column(name = "level_num", nullable = false)
    private Integer levelNum;

    @Column(name = "position_num", nullable = false)
    private Integer positionNum;

    @Column(name = "grid_x")
    private Integer gridX;

    @Column(name = "grid_y")
    private Integer gridY;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer capacity = 1;

    @Column(name = "current_load", columnDefinition = "INT DEFAULT 0")
    private Integer currentLoad = 0;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'AVAILABLE'")
    private ShelfStatus status = ShelfStatus.AVAILABLE;

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

    public enum ShelfStatus {
        AVAILABLE, FULL, MAINTENANCE
    }
}
