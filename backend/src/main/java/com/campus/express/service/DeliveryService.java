package com.campus.express.service;

import com.campus.express.algorithm.CampusPathPlanningService;
import com.campus.express.algorithm.PathPoint;
import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.repository.DeliveryTaskRepository;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryTaskRepository taskRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final CampusPathPlanningService pathPlanningService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DeliveryTask> findByCourierId(Long courierId) {
        return taskRepository.findByCourierId(courierId);
    }

    public List<DeliveryTask> findByStatus(DeliveryTask.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<DeliveryTask> findPendingForGrab() {
        return taskRepository.findByStatus(DeliveryTask.TaskStatus.PENDING);
    }

    public List<DeliveryTask> findByCourierUsername(String username) {
        User courier = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return taskRepository.findByCourierId(courier.getId());
    }

    public List<DeliveryTask> findAll() {
        return taskRepository.findAll();
    }

    /**
     * 学生端预约配送：学生为自己的待取件包裹预约上门配送
     */
    @Transactional
    public DeliveryTask scheduleDeliveryForStudent(Long packageId, String destination, String username) {
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));
        if (!pkg.getStudentId().equals(student.getId())) {
            throw new RuntimeException("无权为该包裹预约配送");
        }
        return createDeliveryTask(packageId, destination);
    }

    @Transactional
    public DeliveryTask createDeliveryTask(Long packageId, String destination) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));

        if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
            throw new RuntimeException("包裹状态不允许创建配送任务");
        }

        List<PathPoint> path = pathPlanningService.findShortestPath("STATION_1", destination);
        var result = pathPlanningService.optimizePathForDelivery(path, pkg.getSize());

        DeliveryTask task = new DeliveryTask();
        task.setPackageId(packageId);
        task.setDestination(destination);
        task.setStatus(DeliveryTask.TaskStatus.PENDING);
        task.setEstimatedDistance(BigDecimal.valueOf(result.getTotalDistance()));
        task.setEstimatedTime(BigDecimal.valueOf(result.getEstimatedTime()));
        task.setPriority(1);

        try {
            task.setPathJson(objectMapper.writeValueAsString(
                result.getOptimizedPath().stream()
                    .map(p -> new int[]{p.x(), p.y()})
                    .toList()));
        } catch (JsonProcessingException e) {
            task.setPathJson("[]");
        }

        task = taskRepository.save(task);

        pkg.setStatus(Package.PackageStatus.OUT_FOR_DELIVERY);
        pkg.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes((long) result.getEstimatedTime()));
        packageRepository.save(pkg);

        return task;
    }

    /**
     * 快递员抢单：快递员将待分配的配送任务抢为自己执行
     */
    @Transactional
    public DeliveryTask grabTask(Long taskId, String courierUsername) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));
        if (task.getStatus() != DeliveryTask.TaskStatus.PENDING) {
            throw new RuntimeException("该任务已被抢或已完成");
        }
        User courier = userRepository.findByUsername(courierUsername)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (courier.getRole() != User.UserRole.COURIER) {
            throw new RuntimeException("仅快递员可抢单");
        }
        task.setCourierId(courier.getId());
        task.setStatus(DeliveryTask.TaskStatus.ASSIGNED);
        task.setStartedAt(LocalDateTime.now());
        Package pkg = packageRepository.findById(task.getPackageId()).orElseThrow();
        pkg.setCourierId(courier.getId());
        packageRepository.save(pkg);
        return taskRepository.save(task);
    }

    /**
     * 快递员开始配送：将已分配任务转为配送中
     */
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
            throw new RuntimeException("任务状态不允许开始配送");
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
            .orElseThrow(() -> new RuntimeException("快递员不存在"));

        if (courier.getRole() != User.UserRole.COURIER) {
            throw new RuntimeException("该用户不是快递员");
        }

        task.setCourierId(courierId);
        task.setStatus(DeliveryTask.TaskStatus.ASSIGNED);

        Package pkg = packageRepository.findById(task.getPackageId()).orElseThrow();
        pkg.setCourierId(courierId);

        packageRepository.save(pkg);
        return taskRepository.save(task);
    }

    @Transactional
    public DeliveryTask completeDelivery(Long taskId) {
        DeliveryTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("配送任务不存在"));

        task.setStatus(DeliveryTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Package pkg = packageRepository.findById(task.getPackageId()).orElseThrow();
        pkg.setStatus(Package.PackageStatus.DELIVERED);
        pkg.setActualDeliveryTime(LocalDateTime.now());

        packageRepository.save(pkg);
        return taskRepository.save(task);
    }
}
