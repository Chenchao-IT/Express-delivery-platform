package com.campus.express.repository;

import com.campus.express.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long> {

    Optional<Package> findByTrackingNumber(String trackingNumber);

    List<Package> findByStudentId(Long studentId);

    List<Package> findByStudentIdAndStatus(Long studentId, Package.PackageStatus status);

    List<Package> findByStatus(Package.PackageStatus status);

    boolean existsByTrackingNumber(String trackingNumber);
}
