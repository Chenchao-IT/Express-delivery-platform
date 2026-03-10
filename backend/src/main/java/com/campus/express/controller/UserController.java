package com.campus.express.controller;

import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String role) {
        List<User> users = role != null && !role.isEmpty()
            ? userRepository.findByRole(User.UserRole.valueOf(role))
            : userRepository.findAll();

        List<Map<String, Object>> result = users.stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "realName", u.getRealName() != null ? u.getRealName() : "",
                "role", u.getRole().name(),
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "college", u.getCollege() != null ? u.getCollege() : ""
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/couriers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> listCouriers() {
        return ResponseEntity.ok(userRepository.findByRole(User.UserRole.COURIER));
    }
}
