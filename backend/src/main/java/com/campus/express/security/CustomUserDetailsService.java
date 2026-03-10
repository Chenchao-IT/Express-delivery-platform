package com.campus.express.security;

import com.campus.express.entity.User;
import com.campus.express.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        List<org.springframework.security.core.GrantedAuthority> authorities = Stream.of("ROLE_" + user.getRole().name())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
        return new CustomUserDetails(
            user.getUsername(),
            user.getPassword(),
            authorities,
            user.getId()
        );
    }
}
