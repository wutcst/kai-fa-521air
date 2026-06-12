package com.snake.controller;

import com.snake.dto.AuthResponse;
import com.snake.dto.AuthResponse.UserInfo;
import com.snake.dto.LoginRequest;
import com.snake.dto.RegisterRequest;
import com.snake.entity.SysUser;
import com.snake.repository.SysUserRepository;
import com.snake.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/** 认证控制器 处理登录、注册、退出、获取当前用户 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "01-认证管理", description = "用户注册、登录、退出、获取当前用户信息")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(
            SysUserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT token 和用户信息")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "登录成功，返回 token 和用户信息",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "用户名或密码为空",
                content =
                        @Content(
                                examples = @ExampleObject(value = "{\"message\":\"用户名和密码不能为空\"}"))),
        @ApiResponse(
                responseCode = "401",
                description = "用户名或密码错误",
                content =
                        @Content(examples = @ExampleObject(value = "{\"message\":\"用户名或密码错误\"}"))),
        @ApiResponse(
                responseCode = "403",
                description = "账号已被禁用",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"账号已被禁用\"}")))
    })
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "账号已被禁用"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = toUserInfo(user);

        log.info("User login: {} (id={})", user.getUsername(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户（用户名3-50字符，密码至少6位），返回 JWT token")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "注册成功",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "参数校验失败（用户名已存在/密码过短等）",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"用户名已存在\"}")))
    })
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
        user.setNickname(
                request.getNickname() != null ? request.getNickname() : request.getUsername());
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
    @Operation(summary = "获取当前用户信息", description = "需要 JWT token，返回当前登录用户的基本信息（不含密码）")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "成功返回用户信息",
                content = @Content(schema = @Schema(implementation = AuthResponse.UserInfo.class))),
        @ApiResponse(
                responseCode = "401",
                description = "未登录或 token 无效",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"未登录\"}")))
    })
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户不存在"));
        }
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "JWT 无状态，仅返回成功消息，客户端需自行删除 token")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "退出成功",
                content = @Content(examples = @ExampleObject(value = "{\"message\":\"退出成功\"}")))
    })
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
                user.getTotalScore());
    }
}
