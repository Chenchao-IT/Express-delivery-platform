package com.campus.express.controller;

import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<List<Package>> getMyPackages(@AuthenticationPrincipal UserDetails user) {
        Long studentId = userRepository.findByUsername(user.getUsername())
            .map(User::getId)
            .orElse(null);
        if (studentId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(packageService.findByStudentId(studentId));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<?> trackPackage(@PathVariable String trackingNumber) {
        return packageService.findByTrackingNumber(trackingNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<Package> pickupPackage(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(packageService.pickupPackage(id, user.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<List<Package>> listPackages(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                return ResponseEntity.ok(packageService.findByStatus(Package.PackageStatus.valueOf(status)));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(packageService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<Package> createPackage(@RequestBody Map<String, String> request) {
        String studentUsername = request.get("studentUsername");
        String trackingNumber = request.get("trackingNumber");
        String sizeStr = request.getOrDefault("size", "MEDIUM");
        String shelfCode = request.getOrDefault("shelfCode", "");

        Package.PackageSize size = Package.PackageSize.valueOf(sizeStr);
        Package pkg = packageService.createPackage(studentUsername, trackingNumber, size, shelfCode);
        return ResponseEntity.ok(pkg);
    }
}
