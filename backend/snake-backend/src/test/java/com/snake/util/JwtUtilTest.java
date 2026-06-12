package com.snake.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** JWT 工具类单元测试 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-hs512-algorithm!!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 86400000);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtil.generateToken(1L, "testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void generateToken_ShouldEncodeUserId() {
        String token = jwtUtil.generateToken(42L, "player42");
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void generateToken_ShouldEncodeUsername() {
        String token = jwtUtil.generateToken(1L, "admin");
        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("admin", username);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("invalid-token-string"));
    }

    @Test
    void validateToken_WithEmptyToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_WithNullToken_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void validateToken_WithTamperedToken_ShouldReturnFalse() {
        String token = jwtUtil.generateToken(1L, "testuser");
        // Modify the token - change last character
        String tampered = token.substring(0, token.length() - 2) + "XX";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void getUserIdFromToken_WithMultipleUsers_ShouldReturnCorrectId() {
        String token1 = jwtUtil.generateToken(100L, "user100");
        String token2 = jwtUtil.generateToken(200L, "user200");

        assertEquals(100L, jwtUtil.getUserIdFromToken(token1));
        assertEquals(200L, jwtUtil.getUserIdFromToken(token2));
    }

    @Test
    void getUsernameFromToken_WithMultipleUsers_ShouldReturnCorrectUsername() {
        String token1 = jwtUtil.generateToken(1L, "alpha");
        String token2 = jwtUtil.generateToken(2L, "beta");

        assertEquals("alpha", jwtUtil.getUsernameFromToken(token1));
        assertEquals("beta", jwtUtil.getUsernameFromToken(token2));
    }

    @Test
    void token_ShouldNotBeExpired() {
        // Token with 24h expiration should be valid immediately
        JwtUtil longLived = new JwtUtil(TEST_SECRET, 86400000);
        String token = longLived.generateToken(1L, "test");
        assertTrue(longLived.validateToken(token));
    }

    @Test
    void generateToken_WithSameData_ShouldContainSameClaims() {
        // Both tokens should decode to the same claims regardless of generation time
        String token1 = jwtUtil.generateToken(1L, "test");
        String token2 = jwtUtil.generateToken(1L, "test");
        assertEquals(jwtUtil.getUserIdFromToken(token1), jwtUtil.getUserIdFromToken(token2));
        assertEquals(jwtUtil.getUsernameFromToken(token1), jwtUtil.getUsernameFromToken(token2));
    }
}
