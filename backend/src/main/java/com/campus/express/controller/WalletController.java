package com.campus.express.controller;

import com.campus.express.entity.User;
import com.campus.express.entity.UserWallet;
import com.campus.express.repository.UserRepository;
import com.campus.express.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserRepository userRepository;
    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyWallet(@AuthenticationPrincipal UserDetails user) {
        Long userId = userRepository.findByUsername(user.getUsername())
            .map(User::getId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserWallet w = walletService.ensureWallet(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "balance", w.getBalance(),
            "frozen", w.getFrozen()
        ));
    }
}

