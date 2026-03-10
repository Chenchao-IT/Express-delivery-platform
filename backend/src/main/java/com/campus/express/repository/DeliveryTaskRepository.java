package com.campus.express.repository;

import com.campus.express.entity.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {

    List<DeliveryTask> findByCourierId(Long courierId);

    List<DeliveryTask> findByCourierIdAndStatus(Long courierId, DeliveryTask.TaskStatus status);

    List<DeliveryTask> findByStatus(DeliveryTask.TaskStatus status);

    List<DeliveryTask> findByPackageId(Long packageId);
}
