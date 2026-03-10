package com.campus.express.security;

import com.campus.express.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final int HMAC_SHA256_MIN_BYTES = 32;
    private static final String VERSION_SALT = "campus-salt";

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // 文档 1.2：signingKey = "campus-delivery-secret-2023"；HS256 需至少 32 字节，不足则填充
        byte[] raw = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < HMAC_SHA256_MIN_BYTES) {
            raw = Arrays.copyOf(raw, HMAC_SHA256_MIN_BYTES);
        }
        this.secretKey = Keys.hmacShaKeyFor(raw);
    }

    /** 兼容旧调用：仅 subject，无 version/vhash */
    public String generateToken(String username) {
        return generateToken(username, null, null);
    }

    /** 文档 1.2：生成带 version、vhash 声明的 JWT */
    public String generateToken(String username, Long userId, Integer version) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        var builder = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate);
        if (userId != null && version != null) {
            builder.claim("userId", userId);
            builder.claim("version", version);
            builder.claim("vhash", hashVersion(userId, version));
        }
        return builder.signWith(secretKey).compact();
    }

    /** 文档 1.2：vhash = md5(userId:version:campus-salt) */
    private static String hashVersion(Long userId, Integer version) {
        String input = userId + ":" + version + ":" + VERSION_SALT;
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Object uid = getClaims(token).get("userId");
        if (uid instanceof Number) {
            return ((Number) uid).longValue();
        }
        return null;
    }

    public Integer getVersionFromToken(String token) {
        Object v = getClaims(token).get("version");
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
