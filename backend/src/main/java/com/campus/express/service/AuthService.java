package com.campus.express.service;

import com.campus.express.dto.LoginRequest;
import com.campus.express.dto.LoginResponse;
import com.campus.express.dto.RegisterRequest;
import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import com.campus.express.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        int version = user.getLoginVersion() != null ? user.getLoginVersion() : 1;
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), version);
        return LoginResponse.builder()
            .token(token)
            .username(user.getUsername())
            .role(user.getRole().name())
            .realName(user.getRealName())
            .build();
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(User.UserRole.STUDENT);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setCollege(request.getCollege());
        user.setAddress(request.getAddress());
        user = userRepository.save(user);

        int version = user.getLoginVersion() != null ? user.getLoginVersion() : 1;
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), version);
        return LoginResponse.builder()
            .token(token)
            .username(user.getUsername())
            .role(user.getRole().name())
            .realName(user.getRealName())
            .build();
    }
}
