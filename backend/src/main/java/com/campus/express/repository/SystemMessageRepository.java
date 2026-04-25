package com.campus.express.repository;

import com.campus.express.entity.SystemMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemMessageRepository extends JpaRepository<SystemMessage, Long> {

    List<SystemMessage> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndReadFalse(Long receiverId);
}
