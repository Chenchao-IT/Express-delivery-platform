package com.campus.express.repository;

import com.campus.express.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {

    List<ExternalIdentity> findByUserId(Long userId);

    List<ExternalIdentity> findByUserIdAndSource(Long userId, String source);
}
