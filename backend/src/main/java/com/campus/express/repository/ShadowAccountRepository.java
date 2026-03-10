package com.campus.express.repository;

import com.campus.express.entity.ShadowAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShadowAccountRepository extends JpaRepository<ShadowAccount, Long> {

    List<ShadowAccount> findByOriginalUsernameAndStatus(String originalUsername, ShadowAccount.ShadowStatus status);
}
