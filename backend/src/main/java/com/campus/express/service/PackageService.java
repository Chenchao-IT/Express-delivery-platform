package com.campus.express.service;

import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.entity.VirtualShelf;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
import com.campus.express.repository.VirtualShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final VirtualShelfRepository shelfRepository;
    private final NotificationService notificationService;

    public List<Package> findByStudentId(Long studentId) {
        return packageRepository.findByStudentId(studentId);
    }

    public Optional<Package> findByTrackingNumber(String trackingNumber) {
        return packageRepository.findByTrackingNumber(trackingNumber);
    }

    public List<Package> findByStatus(Package.PackageStatus status) {
        return packageRepository.findByStatus(status);
    }

    public List<Package> findAll() {
        return packageRepository.findAll();
    }

    @Transactional
    public Package createPackage(
        String studentUsername,
        String trackingNumber,
        Package.PackageSize size,
        String shelfCode
    ) {
        User student = userRepository.findByUsername(studentUsername)
            .orElseThrow(() -> new RuntimeException("学生不存在"));

        String finalTrackingNumber = trackingNumber == null || trackingNumber.isBlank()
            ? "PKG" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
            : trackingNumber.trim().toUpperCase();

        if (packageRepository.existsByTrackingNumber(finalTrackingNumber)) {
            throw new RuntimeException("该单号已存在，请勿重复扫描");
        }

        String finalShelfCode = shelfCode;
        if (finalShelfCode == null || finalShelfCode.isBlank() || shelfRepository.findByShelfCode(finalShelfCode).isEmpty()) {
            finalShelfCode = allocateShelf();
        }

        Package pkg = new Package();
        pkg.setTrackingNumber(finalTrackingNumber);
        pkg.setStudentId(student.getId());
        pkg.setSize(size == null ? Package.PackageSize.MEDIUM : size);
        pkg.setStatus(Package.PackageStatus.IN_STORAGE);
        pkg.setShelfCode(finalShelfCode);
        pkg.setPickupCode(generatePickupCode());
        pkg.setStorageTime(LocalDateTime.now());

        Package saved = packageRepository.save(pkg);
        notificationService.createMessage(
            student.getId(),
            "package_inbound",
            "包裹已入库",
            "包裹 " + saved.getTrackingNumber()
                + " 已入库，货架位置：" + saved.getShelfCode()
                + "，提货码：" + saved.getPickupCode()
        );
        return saved;
    }

    @Transactional
    public Package pickupPackage(Long packageId, String username) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!pkg.getStudentId().equals(user.getId())) {
            throw new RuntimeException("无权领取该包裹");
        }

        if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE
            && pkg.getStatus() != Package.PackageStatus.DELIVERED) {
            throw new RuntimeException("包裹当前状态不允许取件");
        }

        pkg.setStatus(Package.PackageStatus.PICKED_UP);
        pkg.setActualDeliveryTime(LocalDateTime.now());
        return packageRepository.save(pkg);
    }

    private String allocateShelf() {
        List<VirtualShelf> available = shelfRepository.findByStatus(VirtualShelf.ShelfStatus.AVAILABLE);
        if (!available.isEmpty()) {
            return available.get(0).getShelfCode();
        }
        return "A-01-1-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 1000));
    }

    private String generatePickupCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }
}
