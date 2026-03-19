package com.campus.express.repository;

import com.campus.express.entity.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {

    List<DeliveryTask> findByCourierId(Long courierId);

    List<DeliveryTask> findByCourierIdAndStatus(Long courierId, DeliveryTask.TaskStatus status);

    List<DeliveryTask> findByStatus(DeliveryTask.TaskStatus status);

    List<DeliveryTask> findByPackageId(Long packageId);

    List<DeliveryTask> findByTypeAndStatus(DeliveryTask.TaskType type, DeliveryTask.TaskStatus status);

    List<DeliveryTask> findByPublisherId(Long publisherId);

    List<DeliveryTask> findByCourierIdAndType(Long courierId, DeliveryTask.TaskType type);

    Optional<DeliveryTask> findFirstByPackageIdAndTypeAndStatusIn(
        Long packageId,
        DeliveryTask.TaskType type,
        List<DeliveryTask.TaskStatus> statuses
    );
}
