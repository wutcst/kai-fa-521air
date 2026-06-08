package com.snake.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // 直接在代码中创建密钥，避免 @Value 属性加载问题
    private static final SecretKey JWT_SECRET_KEY = Keys.hmacShaKeyFor(
            "snake-game-jwt-secret-key-must-be-at-least-256-bits-long-for-hs512"
                    .getBytes(StandardCharsets.UTF_8));

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OncePerRequestFilter jwtFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {

                String header = request.getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    String token = header.substring(7);
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(JWT_SECRET_KEY)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        Long userId = Long.parseLong(claims.getSubject());
                        String username = claims.get("username", String.class);
                        var auth = new UsernamePasswordAuthenticationToken(userId, username, List.of());
                        auth.setDetails(username);
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("JWT auth OK: userId={}, username={}", userId, username);
                    } catch (Exception e) {
                        log.warn("JWT auth failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                }
                chain.doFilter(request, response);
            }
        };

        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/rooms/list").permitAll()
                .requestMatchers("/api/rooms/online-count").permitAll()
                .requestMatchers("/api/games/**").permitAll()
                .requestMatchers("/api/ranking").permitAll()
                .requestMatchers("/api/ranking/**").permitAll()
                .requestMatchers("/api/matchmaking/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, AuthorizationFilter.class);

        return http.build();
    }

    /**
     * WebSocket 安全链：不经过 JWT 过滤器，直接放行所有 WebSocket 连接
     */
    @Bean
    @Order(2)
    public SecurityFilterChain wsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/ws/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
