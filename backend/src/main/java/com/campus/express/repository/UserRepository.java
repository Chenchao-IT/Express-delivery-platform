package com.campus.express.repository;

import com.campus.express.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findByRole(User.UserRole role);

    /** 多源冲突检测：同一手机被其他账号使用（排除指定 id） */
    java.util.List<User> findByPhoneAndIdNot(String phone, Long excludeId);

    /** 多源冲突检测：同一邮箱被其他账号使用（排除指定 id） */
    java.util.List<User> findByEmailAndIdNot(String email, Long excludeId);
}
