package com.campus.express.controller;

import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal UserDetails user) {
        Long userId = userRepository.findByUsername(user.getUsername()).map(User::getId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return notificationService.subscribe(userId);
    }
}

