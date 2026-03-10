package com.campus.express.controller;

import com.campus.express.dto.AppealRequest;
import com.campus.express.dto.AppealResult;
import com.campus.express.dto.ReviewResult;
import com.campus.express.entity.IdentityAppeal;
import com.campus.express.repository.UserRepository;
import com.campus.express.security.CustomUserDetails;
import com.campus.express.service.IdentityAppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档 2.2：身份申诉 - 提交申诉、管理员审核
 */
@RestController
@RequiredArgsConstructor
public class AppealController {

    private final IdentityAppealService identityAppealService;
    private final UserRepository userRepository;

    /** 文档 2.2：提交身份申诉（需登录，userId 从 principal 或 DB 取） */
    @PostMapping("/auth/appeal")
    public ResponseEntity<AppealResult> submitAppeal(
            @Valid @RequestBody AppealRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        request.setUserId(userId);
        AppealResult result = identityAppealService.submitAppeal(request);
        return ResponseEntity.ok(result);
    }

    /** 我的申诉列表 */
    @GetMapping("/auth/appeals/me")
    public ResponseEntity<List<IdentityAppeal>> myAppeals(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(identityAppealService.findMyAppeals(userId));
    }

    private Long resolveUserId(UserDetails principal) {
        if (principal instanceof CustomUserDetails) {
            Long id = ((CustomUserDetails) principal).getUserId();
            if (id != null) return id;
        }
        return userRepository.findByUsername(principal.getUsername())
            .map(com.campus.express.entity.User::getId)
            .orElse(null);
    }

    /** 文档 2.2：管理员 - 待审核申诉列表 */
    @GetMapping("/admin/appeals")
    public ResponseEntity<List<IdentityAppeal>> listPendingAppeals() {
        return ResponseEntity.ok(identityAppealService.findPendingAppeals());
    }

    /** 文档 2.2：管理员 - 审核申诉 */
    @PostMapping("/admin/appeals/{id}/review")
    public ResponseEntity<ReviewResult> reviewAppeal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Boolean approved = body.get("approved") instanceof Boolean ? (Boolean) body.get("approved") : null;
        String comments = body.get("comments") != null ? body.get("comments").toString() : null;
        if (approved == null) {
            return ResponseEntity.badRequest().build();
        }
        ReviewResult result = identityAppealService.reviewAppeal(id, principal != null ? principal.getUsername() : null, approved, comments);
        return ResponseEntity.ok(result);
    }
}
