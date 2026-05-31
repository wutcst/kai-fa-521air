package com.snake.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 生成和验证登录令牌
 * 注意：Spring Boot 4.0.x / Spring Security 7.x 环境下测试
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:snake-game-default-secret-key-change-in-production-123456}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        log.info("JwtUtil: secret length={} bytes, first 8 chars='{}'", keyBytes.length, secret.substring(0, Math.min(8, secret.length())));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    public void init() {
        // 自检：生成本地 token 并验证，确认密钥配置正确
        try {
            String testToken = generateToken(1L, "test");
            boolean valid = validateToken(testToken);
            log.info("JwtUtil self-test: generate+validate = {}", valid ? "OK" : "FAILED");
            if (!valid) {
                log.warn("JwtUtil self-test FAILED - generated token could not be validated!");
            }
        } catch (Exception e) {
            log.error("JwtUtil self-test error: {}", e.getMessage(), e);
        }
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT validate failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
