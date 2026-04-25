package com.campus.express.service;

import com.campus.express.algorithm.CampusPathPlanningService;
import com.campus.express.algorithm.PathPoint;
import com.campus.express.entity.ConflictLog;
import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.repository.ConflictLogRepository;
import com.campus.express.repository.DeliveryTaskRepository;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryTaskRepository taskRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final CampusPathPlanningService pathPlanningService;
    private final WalletService walletService;
    private final ConflictLogRepository conflictLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DeliveryTask> findByCourierId(Long courierId) {
        return taskRepository.findByCourierId(courierId);
    }

    public List<DeliveryTask> findByStatus(DeliveryTask.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<DeliveryTask> findPendingForGrab() {
        return taskRepository.findByStatus(DeliveryTask.TaskStatus.PENDING).stream()
            .filter(task -> task.getType() == DeliveryTask.TaskType.SCHEDULED)
            .toList();
    }

    public List<DeliveryTask> findByCourierUsername(String username) {
        User courier = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return taskRepository.findByCourierId(courier.getId());
    }

    public List<DeliveryTask> findAll() {
        return taskRepository.findAll();
    }

    @Transactional
    public DeliveryTask scheduleDeliveryForStudent(Long packageId, String destination, String username) {
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));

        if (!pkg.getStudentId().equals(student.getId())) {
            throw new RuntimeException("无权为该包裹预约配送");
        }

        DeliveryTask saved = createDeliveryTask(packageId, destination);
        saved.setPublisherId(student.getId());
        saved.setType(DeliveryTask.TaskType.SCHEDULED);
        saved = taskRepository.save(saved);

        notificationService.createMessage(
            student.getId(),
            "delivery_scheduled",
            "预约配送成功",
            "包裹 " + pkg.getTrackingNumber() + " 已预约配送至 " + destination
        );
        return saved;
    }

    @Transactional
    public DeliveryTask createDeliveryTask(Long packageId, String destination) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));

        if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
            throw new RuntimeException("包裹当前状态不允许创建配送任务");
        }

        List<PathPoint> path = pathPlanningService.findShortestPath("STATION_1", destination);
        var result = pathPlanningService.optimizePathForDelivery(path, pkg.getSize());

        DeliveryTask task = new DeliveryTask();
        task.setPackageId(packageId);
        task.setDestination(destination);
        task.setStatus(DeliveryTask.TaskStatus.PENDING);
        task.setType(DeliveryTask.TaskType.SCHEDULED);
        task.setEstimatedDistance(BigDecimal.valueOf(result.getTotalDistance()));
        task.setEstimatedTime(BigDecimal.valueOf(result.getEstimatedTime()));
        task.setPriority(1);

        try {
            task.setPathJson(objectMapper.writeValueAsString(
                result.getOptimizedPath().stream().map(point -> new int[]{point.x(), point.y()}).toList()
            ));
        } catch (JsonProcessingException e) {
            task.setPathJson("[]");
        }

        DeliveryTask saved = taskRepository.save(task);
        pkg.setStatus(Package.PackageStatus.OUT_FOR_DELIVERY);
        pkg.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes((long) result.getEstimatedTime()));
        packageRepository.save(pkg);
        return saved;
    }

    @Transactional
    public DeliveryTask publishRewardTask(Long packageId, String destination, BigDecimal rewardAmount, String username) {
        if (rewardAmount == null) {
            throw new RuntimeException("悬赏金额不能为空");
        }
        rewardAmount = rewardAmount.setScale(2, RoundingMode.HALF_UP);
        if (rewardAmount.signum() <= 0) {
            throw new RuntimeException("悬赏金额必须大于 0");
        }

        User publisher = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (publisher.getRole() != User.UserRole.STUDENT) {
            throw new RuntimeException("仅学生可发布悬赏代取");
        }

        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        if (!pkg.getStudentId().equals(publisher.getId())) {
            throw new RuntimeException("无权为该包裹发布悬赏");
        }
        if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
            throw new RuntimeException("包裹当前状态不允许发布悬赏代取");
        }

        taskRepository.findFirstByPackageIdAndTypeAndStatusIn(
            packageId,
            DeliveryTask.TaskType.REWARD,
            List.of(DeliveryTask.TaskStatus.PENDING, DeliveryTask.TaskStatus.ASSIGNED, DeliveryTask.TaskStatus.IN_PROGRESS)
        ).ifPresent(task -> {
            throw new RuntimeException("该包裹已有进行中的悬赏任务");
        });

        walletService.ensureMinimumBalance(publisher.getId(), rewardAmount);
        walletService.freeze(publisher.getId(), rewardAmount);

        DeliveryTask task = new DeliveryTask();
        task.setPackageId(packageId);
        task.setPublisherId(publisher.getId());
        task.setDestination(destination);
        task.setType(DeliveryTask.TaskType.REWARD);
        task.setRewardAmount(rewardAmount);
        task.setStatus(DeliveryTask.TaskStatus.PENDING);
        task.setPriority(2);

        DeliveryTask saved = taskRepository.save(task);
        notificationService.createMessage(
            publisher.getId(),
            "reward_published",
            "悬赏任务已发布",
            "包裹 " + pkg.getTrackingNumber() + " 的悬赏任务已发布，金额 ¥" + saved.getRewardAmount()
        );
        return saved;
    }

    public List<DeliveryTask> listPendingRewardTasks() {
        return taskRepository.findByTypeAndStatus(DeliveryTask.TaskType.REWARD, DeliveryTask.TaskStatus.PENDING);
    }

    public List<DeliveryTask> listMyRewardTasks(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return taskRepository.findByCourierIdAndType(user.getId(), DeliveryTask.TaskType.REWARD);
    }

    public List<DeliveryTask> listPublishedRewardTasks(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return taskRepository.findByPublisherId(user.getId()).stream()
            .filter(task -> task.getType() == DeliveryTask.TaskType.REWARD)
            .toList();
    }

    @Transactional
    public DeliveryTask acceptRewardTask(Long taskId, String username) {
        User taker = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (taker.getRole() != User.UserRole.STUDENT && taker.getRole() != User.UserRole.COURIER) {
            throw new RuntimeException("无权接取该任务");
        }

        try {
            DeliveryTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
            if (task.getType() != DeliveryTask.TaskType.REWARD) {
                throw new RuntimeException("当前任务不是悬赏任务");
            }
            if (task.getStatus() != DeliveryTask.TaskStatus.PENDING) {
                throw new RuntimeException("任务已被接取或已结束");
            }
            if (task.getPublisherId() != null && task.getPublisherId().equals(taker.getId())) {
                throw new RuntimeException("不能接取自己发布的悬赏任务");
            }

            task.setCourierId(taker.getId());
            task.setStatus(DeliveryTask.TaskStatus.ASSIGNED);
            task.setStartedAt(LocalDateTime.now());
            DeliveryTask saved = taskRepository.save(task);

            notificationService.createMessage(
                saved.getPublisherId(),
                "reward_accepted",
                "悬赏任务已接单",
                "包裹 #" + saved.getPackageId() + " 已被接单，请留意后续配送进度"
            );
            return saved;
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
            conflictLogRepository.save(ConflictLog.builder()
                .userId(taker.getId())
                .conflictType("TASK_GRAB")
                .conflictingFields("status,version")
                .resolution("REJECTED")
                .build());
            throw new RuntimeException("手慢了，任务已被其他人接单");
        }
    }

    @Transactional
    public DeliveryTask cancelRewardTask(Long taskId, String username) {
        User publisher = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getType() != DeliveryTask.TaskType.REWARD) {
            throw new RuntimeException("当前任务不是悬赏任务");
        }
        if (task.getPublisherId() == null || !task.getPublisherId().equals(publisher.getId())) {
            throw new RuntimeException("无权取消该任务");
        }
        if (task.getStatus() != DeliveryTask.TaskStatus.PENDING) {
            throw new RuntimeException("任务已被接单，无法取消");
        }

        if (task.getRewardAmount() != null) {
            walletService.unfreezeToBalance(publisher.getId(), task.getRewardAmount());
        }
        task.setStatus(DeliveryTask.TaskStatus.FAILED);
        task.setCompletedAt(LocalDateTime.now());

        DeliveryTask saved = taskRepository.save(task);
        notificationService.createMessage(
            publisher.getId(),
            "reward_cancelled",
            "悬赏任务已取消",
            "包裹 #" + saved.getPackageId() + " 的悬赏任务已取消，冻结金额已退回"
        );
        return saved;
    }

    @Transactional
    public DeliveryTask completeRewardTask(Long taskId, String username) {
        User actor = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getType() != DeliveryTask.TaskType.REWARD) {
            throw new RuntimeException("当前任务不是悬赏任务");
        }
        if (task.getCourierId() == null || !task.getCourierId().equals(actor.getId())) {
            throw new RuntimeException("无权完成该任务");
        }
        if (task.getStatus() == DeliveryTask.TaskStatus.COMPLETED) {
            return task;
        }
        if (task.getStatus() != DeliveryTask.TaskStatus.ASSIGNED && task.getStatus() != DeliveryTask.TaskStatus.IN_PROGRESS) {
            throw new RuntimeException("任务当前状态不允许完成");
        }

        task.setStatus(DeliveryTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Package pkg = packageRepository.findById(task.getPackageId())
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        pkg.setStatus(Package.PackageStatus.DELIVERED);
        pkg.setActualDeliveryTime(LocalDateTime.now());
        packageRepository.save(pkg);

        if (task.getPublisherId() != null && task.getRewardAmount() != null && task.getRewardAmount().signum() > 0) {
            walletService.transferFromFrozenToBalance(task.getPublisherId(), actor.getId(), task.getRewardAmount());
        }

        DeliveryTask saved = taskRepository.save(task);
        notificationService.createMessage(
            saved.getPublisherId(),
            "reward_completed",
            "悬赏任务已完成",
            "包裹 " + pkg.getTrackingNumber() + " 已完成代取，悬赏金额已结算"
        );
        return saved;
    }

    @Transactional
    public DeliveryTask grabTask(Long taskId, String courierUsername) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));
        if (task.getStatus() != DeliveryTask.TaskStatus.PENDING) {
            throw new RuntimeException("该任务已被抢单或已完成");
        }
        if (task.getType() == DeliveryTask.TaskType.REWARD) {
            throw new RuntimeException("悬赏任务请使用悬赏接单接口");
        }

        User courier = userRepository.findByUsername(courierUsername)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (courier.getRole() != User.UserRole.COURIER) {
            throw new RuntimeException("仅代取员可抢单");
        }

        task.setCourierId(courier.getId());
        task.setStatus(DeliveryTask.TaskStatus.ASSIGNED);
        task.setStartedAt(LocalDateTime.now());

        Package pkg = packageRepository.findById(task.getPackageId())
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        pkg.setCourierId(courier.getId());
        packageRepository.save(pkg);

        return taskRepository.save(task);
    }

    @Transactional
    public DeliveryTask startDelivery(Long taskId, String courierUsername) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));
        User courier = userRepository.findByUsername(courierUsername)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!courier.getId().equals(task.getCourierId())) {
            throw new RuntimeException("无权操作此任务");
        }
        if (task.getStatus() != DeliveryTask.TaskStatus.ASSIGNED) {
            throw new RuntimeException("任务当前状态不允许开始配送");
        }

        task.setStatus(DeliveryTask.TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Transactional
    public DeliveryTask assignCourier(Long taskId, Long courierId) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));
        User courier = userRepository.findById(courierId)
            .orElseThrow(() -> new RuntimeException("代取员不存在"));

        if (courier.getRole() != User.UserRole.COURIER) {
            throw new RuntimeException("该用户不是代取员");
        }

        task.setCourierId(courierId);
        task.setStatus(DeliveryTask.TaskStatus.ASSIGNED);

        Package pkg = packageRepository.findById(task.getPackageId())
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        pkg.setCourierId(courierId);
        packageRepository.save(pkg);

        return taskRepository.save(task);
    }

    @Transactional
    public DeliveryTask completeDelivery(Long taskId) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));

        if (task.getType() == DeliveryTask.TaskType.REWARD) {
            throw new RuntimeException("悬赏任务请使用悬赏完成接口");
        }
        if (task.getStatus() == DeliveryTask.TaskStatus.COMPLETED) {
            return task;
        }

        task.setStatus(DeliveryTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Package pkg = packageRepository.findById(task.getPackageId())
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        pkg.setStatus(Package.PackageStatus.DELIVERED);
        pkg.setActualDeliveryTime(LocalDateTime.now());
        packageRepository.save(pkg);

        DeliveryTask saved = taskRepository.save(task);
        notificationService.createMessage(
            pkg.getStudentId(),
            "delivery_completed",
            "包裹已送达",
            "包裹 " + pkg.getTrackingNumber() + " 已送达，请及时确认签收"
        );
        return saved;
    }
}
