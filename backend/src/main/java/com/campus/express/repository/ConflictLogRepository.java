package com.campus.express.repository;

import com.campus.express.entity.ConflictLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConflictLogRepository extends JpaRepository<ConflictLog, Long> {

    List<ConflictLog> findByUserIdOrderByLoggedAtDesc(Long userId, Pageable pageable);
}
