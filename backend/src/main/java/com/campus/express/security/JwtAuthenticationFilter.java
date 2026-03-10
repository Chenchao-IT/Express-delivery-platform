package com.campus.express.security;

import com.campus.express.service.EmergencyPassAuthService;
import com.campus.express.service.LoginVersionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Autowired(required = false)
    private LoginVersionManager loginVersionManager;

    @Autowired(required = false)
    private EmergencyPassAuthService emergencyPassAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (emergencyPassAuthService != null && emergencyPassAuthService.isEmergencyMode()) {
                EmergencyPassAuthService.EmergencyAuthResult result = emergencyPassAuthService.authenticate(request, jwt);
                if (result.getStatus() == EmergencyPassAuthService.EmergencyAuthResult.NORMAL) {
                    // 非紧急或路径不在策略内，走正常流程
                } else if (result.getStatus() == EmergencyPassAuthService.EmergencyAuthResult.GRANTED_NO_AUTH) {
                    filterChain.doFilter(request, response);
                    return;
                } else if (result.getStatus() == EmergencyPassAuthService.EmergencyAuthResult.GRANTED && result.getUsername() != null) {
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(result.getUsername());
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception e) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"message\":\"紧急模式：用户不存在\"}");
                        return;
                    }
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\":\"" + (result.getMessage() != null ? result.getMessage() : "未授权") + "\"}");
                    return;
                }
            }

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                Integer version = jwtTokenProvider.getVersionFromToken(jwt);
                if (loginVersionManager != null && userId != null && version != null) {
                    if (!loginVersionManager.validateTokenVersion(userId, version)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"message\":\"登录已失效，请重新登录\"}");
                        return;
                    }
                }
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (Exception e) {
                    // 文档 2.2：关键冲突时可能仅凭 token 访问申诉接口，用 token 内 username+userId 设最小认证
                    if (userId != null && StringUtils.hasText(username)) {
                        userDetails = CustomUserDetails.fromToken(username, userId);
                    } else {
                        throw e;
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
