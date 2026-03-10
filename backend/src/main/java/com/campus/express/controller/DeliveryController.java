package com.campus.express.controller;

import com.campus.express.algorithm.CampusPathPlanningService;
import com.campus.express.entity.DeliveryTask;
import com.campus.express.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CampusPathPlanningService pathPlanningService;

    @GetMapping("/destinations")
    public ResponseEntity<java.util.Set<String>> getDestinations() {
        return ResponseEntity.ok(pathPlanningService.getAvailableDestinations());
    }

    /** 待抢单列表（快递员可见） */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listPending() {
        return ResponseEntity.ok(deliveryService.findPendingForGrab());
    }

    /** 我的配送任务（快递员可见） */
    @GetMapping("/my")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listMyDeliveries(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deliveryService.findByCourierUsername(user.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listDeliveries(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                return ResponseEntity.ok(
                    deliveryService.findByStatus(DeliveryTask.TaskStatus.valueOf(status)));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(deliveryService.findAll());
    }

    /** 学生端预约配送：学生为自己的包裹预约上门配送 */
    @PostMapping("/schedule")
    public ResponseEntity<DeliveryTask> scheduleDelivery(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails user) {
        Long packageId = Long.valueOf(request.get("packageId").toString());
        String destination = request.get("destination").toString();
        DeliveryTask task = deliveryService.scheduleDeliveryForStudent(
            packageId, destination, user.getUsername());
        return ResponseEntity.ok(task);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> createDelivery(@RequestBody Map<String, Object> request) {
        Long packageId = Long.valueOf(request.get("packageId").toString());
        String destination = request.get("destination").toString();
        DeliveryTask task = deliveryService.createDeliveryTask(packageId, destination);
        return ResponseEntity.ok(task);
    }

    /** 快递员抢单：将待分配任务抢为自己执行 */
    @PutMapping("/{id}/grab")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> grabTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        DeliveryTask task = deliveryService.grabTask(id, user.getUsername());
        return ResponseEntity.ok(task);
    }

    /** 快递员开始配送 */
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> startDelivery(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        DeliveryTask task = deliveryService.startDelivery(id, user.getUsername());
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryTask> assignCourier(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        Long courierId = request.get("courierId");
        DeliveryTask task = deliveryService.assignCourier(id, courierId);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> completeDelivery(@PathVariable Long id) {
        DeliveryTask task = deliveryService.completeDelivery(id);
        return ResponseEntity.ok(task);
    }
}
