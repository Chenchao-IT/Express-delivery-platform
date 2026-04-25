package com.campus.express.service;

import com.campus.express.config.EmergencyPolicyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalWriteStorage {

    private final EmergencyPolicyProperties policy;
    private BlockingQueue<LocalWriteRecord> writeQueue;

    private BlockingQueue<LocalWriteRecord> getQueue() {
        if (writeQueue == null) {
            synchronized (this) {
                if (writeQueue == null) {
                    int max = policy.getLocalWrites() != null ? policy.getLocalWrites().getMaxPending() : 1000;
                    writeQueue = new LinkedBlockingQueue<>(max);
                }
            }
        }
        return writeQueue;
    }

    public boolean recordWrite(String operation, Map<String, Object> data, String userId) {
        if (policy.getLocalWrites() == null || !policy.getLocalWrites().isEnabled()) {
            return false;
        }

        LocalWriteRecord record = LocalWriteRecord.builder()
            .id(UUID.randomUUID().toString())
            .operation(operation)
            .data(data)
            .userId(userId)
            .timestamp(LocalDateTime.now())
            .synced(false)
            .build();

        boolean offered = getQueue().offer(record);
        if (!offered) {
            log.warn("本地写队列已满，丢弃操作: {}", operation);
        }
        return offered;
    }

    public SyncResult syncToDatabase() {
        SyncResult result = new SyncResult();
        BlockingQueue<LocalWriteRecord> queue = getQueue();
        List<LocalWriteRecord> batch = new ArrayList<>();
        queue.drainTo(batch);

        for (LocalWriteRecord record : batch) {
            try {
                boolean success = applyToDatabase(record);
                if (success) {
                    result.incrementSuccess();
                    log.info("本地写已同步: {} id={}", record.getOperation(), record.getId());
                } else {
                    result.incrementFailed();
                    queue.offer(record);
                }
            } catch (Exception e) {
                log.error("同步本地写失败: {} id={}", record.getOperation(), record.getId(), e);
                result.incrementFailed();
                queue.offer(record);
            }
        }

        result.setTotal(batch.size());
        return result;
    }

    private boolean applyToDatabase(LocalWriteRecord record) {
        log.debug("本地写操作暂未实现回放: {}", record.getOperation());
        return true;
    }

    @lombok.Data
    public static class SyncResult {
        private int total;
        private int successCount;
        private int failedCount;

        void incrementSuccess() {
            successCount++;
        }

        void incrementFailed() {
            failedCount++;
        }
    }
}
