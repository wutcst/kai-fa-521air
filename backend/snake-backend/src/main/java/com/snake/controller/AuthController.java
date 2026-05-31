package com.snake.controller;

import com.snake.dto.AuthResponse;
import com.snake.dto.AuthResponse.UserInfo;
import com.snake.dto.LoginRequest;
import com.snake.dto.RegisterRequest;
import com.snake.entity.SysUser;
import com.snake.repository.SysUserRepository;
import com.snake.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注册、退出、获取当前用户
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(SysUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }

        SysUser user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
        if (user.getStatus() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "账号已被禁用"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = toUserInfo(user);

        log.info("User login: {} (id={})", user.getUsername(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }
        if (request.getUsername().length() < 3 || request.getUsername().length() > 50) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名长度须在3-50个字符之间"));
        }
        if (request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码长度不能少于6位"));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setAvatar("");
        user.setLevel(1);
        user.setTotalScore(0);
        user.setTotalGames(0);
        user.setWins(0);
        user.setStatus(1);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = toUserInfo(user);

        log.info("User registered: {} (id={})", user.getUsername(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, userInfo));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户不存在"));
        }
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT 是无状态的，客户端删除 token 即可
        return ResponseEntity.ok(Map.of("message", "退出成功"));
    }

    private UserInfo toUserInfo(SysUser user) {
        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getLevel(),
                user.getTotalScore()
        );
    }
}
