package com.campus.express.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档 3.2：紧急模式下本地写记录
 */
@Data
@Builder
public class LocalWriteRecord {

    private String id;
    private String operation;
    private Map<String, Object> data;
    private String userId;
    private LocalDateTime timestamp;
    private boolean synced;
    private LocalDateTime syncedAt;
}
