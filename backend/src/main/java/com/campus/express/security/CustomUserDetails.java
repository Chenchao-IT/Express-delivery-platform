package com.campus.express.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 扩展 UserDetails，携带 userId，供申诉等接口使用；并在 loadUserByUsername 失败时用 token 内 userId 设置最小认证。
 */
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;
    private final Long userId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long userId) {
        this.username = username;
        this.password = password;
        this.authorities = authorities == null ? Collections.emptyList() : List.copyOf(authorities);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** 仅用 token 内 username + userId 构建最小 principal（关键冲突时仅凭 token 访问申诉接口） */
    public static CustomUserDetails fromToken(String username, Long userId) {
        List<GrantedAuthority> auths = Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"));
        return new CustomUserDetails(username, "", auths, userId);
    }
}
