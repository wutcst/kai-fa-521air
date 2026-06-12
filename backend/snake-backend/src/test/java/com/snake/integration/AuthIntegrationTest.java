package com.snake.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.snake.dto.LoginRequest;
import com.snake.dto.RegisterRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

/** 认证流程集成测试 测试完整的注册 → 登录 → 获取用户信息 → 退出流程 */
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_ShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("int_user_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setNickname("IntTestUser");

        ResponseEntity<Map> response = post("/api/auth/register", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().get("token"));
        // username 在嵌套的 user 对象内（AuthResponse.user.username）
        Map<String, Object> userObj = (Map<String, Object>) response.getBody().get("user");
        assertNotNull(userObj);
        assertEquals(request.getUsername(), userObj.get("username"));
    }

    @Test
    void register_WithExistingUsername_ShouldReturn400() {
        // 先注册一个用户
        String username = "dup_" + System.currentTimeMillis();
        RegisterRequest first = new RegisterRequest();
        first.setUsername(username);
        first.setPassword("password123");
        post("/api/auth/register", first, Map.class);

        // 再次用相同用户名注册
        RegisterRequest second = new RegisterRequest();
        second.setUsername(username);
        second.setPassword("password123");
        ResponseEntity<Map> response = post("/api/auth/register", second, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("用户名已存在", response.getBody().get("message"));
    }

    @Test
    void login_WithCorrectCredentials_ShouldReturnToken() {
        // 先注册
        String username = "login_" + System.currentTimeMillis();
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setPassword("correctPass123");
        post("/api/auth/register", reg, Map.class);

        // 用正确的密码登录
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(username);
        loginReq.setPassword("correctPass123");
        ResponseEntity<Map> response = post("/api/auth/login", loginReq, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("token"));
    }

    @Test
    void login_WithWrongPassword_ShouldReturn401() {
        String username = "wrongpw_" + System.currentTimeMillis();
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setPassword("correctPass123");
        post("/api/auth/register", reg, Map.class);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(username);
        loginReq.setPassword("wrongPassword456");
        ResponseEntity<Map> response =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", loginReq, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void login_WithNonExistentUser_ShouldReturn401() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("no_such_user_" + System.currentTimeMillis());
        loginReq.setPassword("password123");
        ResponseEntity<Map> response =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", loginReq, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void login_WithEmptyCredentials_ShouldReturn400() {
        ResponseEntity<Map> response = post("/api/auth/login", new LoginRequest(), Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("用户名和密码不能为空", response.getBody().get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getMe_WithValidToken_ShouldReturnUserInfo() {
        // 注册获取 token
        String username = "getme_" + System.currentTimeMillis();
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setPassword("password123");
        ResponseEntity<Map> regResponse = post("/api/auth/register", reg, Map.class);
        String token = (String) regResponse.getBody().get("token");

        // 用 token 访问 /api/auth/me
        ResponseEntity<Map> meResponse = getWithToken("/api/auth/me", token, Map.class);

        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertEquals(username, meResponse.getBody().get("username"));
    }

    @Test
    void getMe_WithoutToken_ShouldReturn401() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(baseUrl() + "/api/auth/me", Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("未登录", response.getBody().get("message"));
    }

    @Test
    void register_WithShortPassword_ShouldReturn400() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("short_" + System.currentTimeMillis());
        request.setPassword("12345");
        ResponseEntity<Map> response = post("/api/auth/register", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("密码长度不能少于6位", response.getBody().get("message"));
    }
}
