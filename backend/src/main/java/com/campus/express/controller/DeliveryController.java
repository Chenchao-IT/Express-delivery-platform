package com.campus.express.controller;

import com.campus.express.algorithm.CampusPathPlanningService;
import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CampusPathPlanningService pathPlanningService;
    private final UserRepository userRepository;

    @GetMapping("/destinations")
    public ResponseEntity<Set<String>> getDestinations() {
        return ResponseEntity.ok(pathPlanningService.getAvailableDestinations());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listPending() {
        return ResponseEntity.ok(deliveryService.findPendingForGrab());
    }

    @GetMapping("/reward/pending")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COURIER')")
    public ResponseEntity<List<Map<String, Object>>> listPendingRewards() {
        return ResponseEntity.ok(deliveryService.listPendingRewardTasks().stream().map(this::toRewardView).toList());
    }

    @GetMapping("/reward/my")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COURIER')")
    public ResponseEntity<List<Map<String, Object>>> listMyRewards(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deliveryService.listMyRewardTasks(user.getUsername()).stream().map(this::toRewardView).toList());
    }

    @GetMapping("/reward/published")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Map<String, Object>>> listPublishedRewards(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deliveryService.listPublishedRewardTasks(user.getUsername()).stream().map(this::toRewardView).toList());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listMyDeliveries(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(deliveryService.findByCourierUsername(user.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<List<DeliveryTask>> listDeliveries(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                return ResponseEntity.ok(deliveryService.findByStatus(DeliveryTask.TaskStatus.valueOf(status)));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(deliveryService.findAll());
    }

    @PostMapping("/schedule")
    public ResponseEntity<DeliveryTask> scheduleDelivery(
        @RequestBody Map<String, Object> request,
        @AuthenticationPrincipal UserDetails user
    ) {
        Long packageId = Long.valueOf(String.valueOf(request.get("packageId")));
        String destination = String.valueOf(request.get("destination"));
        return ResponseEntity.ok(deliveryService.scheduleDeliveryForStudent(packageId, destination, user.getUsername()));
    }

    @PostMapping("/reward/publish")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DeliveryTask> publishReward(
        @RequestBody Map<String, Object> request,
        @AuthenticationPrincipal UserDetails user
    ) {
        Long packageId = Long.valueOf(String.valueOf(request.get("packageId")));
        String destination = String.valueOf(request.get("destination"));
        BigDecimal rewardAmount = new BigDecimal(String.valueOf(request.get("rewardAmount")));
        return ResponseEntity.ok(deliveryService.publishRewardTask(packageId, destination, rewardAmount, user.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> createDelivery(@RequestBody Map<String, Object> request) {
        Long packageId = Long.valueOf(String.valueOf(request.get("packageId")));
        String destination = String.valueOf(request.get("destination"));
        return ResponseEntity.ok(deliveryService.createDeliveryTask(packageId, destination));
    }

    @PutMapping("/reward/{id}/accept")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> acceptReward(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(deliveryService.acceptRewardTask(id, user.getUsername()));
    }

    @PutMapping("/reward/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DeliveryTask> cancelReward(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(deliveryService.cancelRewardTask(id, user.getUsername()));
    }

    @PutMapping("/reward/{id}/complete")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> completeReward(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(deliveryService.completeRewardTask(id, user.getUsername()));
    }

    @PutMapping("/{id}/grab")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> grabTask(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(deliveryService.grabTask(id, user.getUsername()));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> startDelivery(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(deliveryService.startDelivery(id, user.getUsername()));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryTask> assignCourier(
        @PathVariable Long id,
        @RequestBody Map<String, Long> request
    ) {
        return ResponseEntity.ok(deliveryService.assignCourier(id, request.get("courierId")));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COURIER')")
    public ResponseEntity<DeliveryTask> completeDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.completeDelivery(id));
    }

    private Map<String, Object> toRewardView(DeliveryTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("packageId", task.getPackageId());
        result.put("publisherId", task.getPublisherId());
        result.put("courierId", task.getCourierId());
        result.put("destination", task.getDestination());
        result.put("rewardAmount", task.getRewardAmount());
        result.put("status", task.getStatus());
        result.put("type", task.getType());
        result.put("estimatedDistance", task.getEstimatedDistance());
        result.put("estimatedTime", task.getEstimatedTime());
        result.put("createdAt", task.getCreatedAt());

        if (task.getPublisherId() != null) {
            userRepository.findById(task.getPublisherId()).ifPresent(user -> {
                result.put("publisherMaskedName", maskName(user));
                result.put("publisherMaskedPhone", maskPhone(user.getPhone()));
            });
        }

        return result;
    }

    private String maskName(User user) {
        String source = user.getRealName() != null && !user.getRealName().isBlank()
            ? user.getRealName()
            : user.getUsername();
        if (source == null || source.isBlank()) {
            return "匿名用户";
        }
        if (source.length() == 1) {
            return source + "*";
        }
        return source.charAt(0) + "**";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "未绑定手机号";
        }
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
