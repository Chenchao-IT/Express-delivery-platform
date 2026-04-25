package com.campus.express.controller;

import com.campus.express.entity.SystemMessage;
import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.security.JwtTokenProvider;
import com.campus.express.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/stream")
    public SseEmitter stream(
        @AuthenticationPrincipal UserDetails user,
        @RequestParam(required = false) String token
    ) {
        return notificationService.subscribe(resolveUserId(user, token));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(@AuthenticationPrincipal UserDetails user) {
        Long userId = resolveUserId(user, null);
        List<SystemMessage> messages = notificationService.listUserMessages(userId);
        return ResponseEntity.ok(Map.of(
            "items", messages,
            "unreadCount", notificationService.unreadCount(userId)
        ));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> readAll(@AuthenticationPrincipal UserDetails user) {
        Long userId = resolveUserId(user, null);
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 0));
    }

    private Long resolveUserId(UserDetails user, String token) {
        String username = null;

        if (user != null) {
            username = user.getUsername();
        } else if (token != null && !token.isBlank() && jwtTokenProvider.validateToken(token)) {
            username = jwtTokenProvider.getUsernameFromToken(token);
        }

        if (username == null || username.isBlank()) {
            throw new RuntimeException("未授权访问通知服务");
        }

        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
