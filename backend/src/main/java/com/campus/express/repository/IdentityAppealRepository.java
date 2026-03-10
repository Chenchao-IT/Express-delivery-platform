package com.campus.express.repository;

import com.campus.express.entity.IdentityAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IdentityAppealRepository extends JpaRepository<IdentityAppeal, Long> {

    List<IdentityAppeal> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<IdentityAppeal> findByStatusOrderBySlaDeadlineAsc(IdentityAppeal.AppealStatus status);

    /** 文档 2.2：查找临近 SLA 截止的待处理申诉（deadline 在 [start, end] 内） */
    List<IdentityAppeal> findByStatusAndSlaDeadlineBetweenOrderBySlaDeadlineAsc(
        IdentityAppeal.AppealStatus status,
        LocalDateTime start,
        LocalDateTime end
    );
}
