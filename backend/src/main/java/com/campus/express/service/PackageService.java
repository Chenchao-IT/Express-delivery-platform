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

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final VirtualShelfRepository shelfRepository;

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
    public Package createPackage(String studentUsername, Package.PackageSize size, String shelfCode) {
        User student = userRepository.findByUsername(studentUsername)
            .orElseThrow(() -> new RuntimeException("学生不存在"));

        String trackingNumber = "PKG" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Optional<VirtualShelf> shelf = shelfRepository.findByShelfCode(shelfCode);
        if (shelf.isEmpty()) {
            shelfCode = allocateShelf();
        }

        Package pkg = new Package();
        pkg.setTrackingNumber(trackingNumber);
        pkg.setStudentId(student.getId());
        pkg.setSize(size);
        pkg.setStatus(Package.PackageStatus.IN_STORAGE);
        pkg.setShelfCode(shelfCode);
        pkg.setStorageTime(LocalDateTime.now());
        return packageRepository.save(pkg);
    }

    @Transactional
    public Package pickupPackage(Long packageId, String username) {
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("包裹不存在"));

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!pkg.getStudentId().equals(user.getId())) {
            throw new RuntimeException("无权取此包裹");
        }

        if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
            throw new RuntimeException("包裹状态不允许取件");
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
        return "A-01-1-" + String.format("%03d", (int)(Math.random() * 100));
    }
}
