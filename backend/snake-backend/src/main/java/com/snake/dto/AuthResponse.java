package com.snake.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 认证响应 */
@Schema(description = "登录/注册响应")
public class AuthResponse {
    @Schema(
            description = "JWT token（后续请求需在 Authorization 头携带）",
            example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    @Schema(description = "用户基本信息")
    private UserInfo user;

    public AuthResponse() {}

    public AuthResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    /** 返回给前端的用户信息（不含密码） */
    @Schema(description = "用户基本信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;

        @Schema(description = "用户名", example = "admin")
        private String username;

        @Schema(description = "昵称", example = "Admin")
        private String nickname;

        @Schema(description = "头像URL", example = "https://example.com/avatar.png")
        private String avatar;

        @Schema(description = "等级", example = "5")
        private int level;

        @Schema(description = "总得分", example = "15000")
        private long totalScore;

        public UserInfo() {}

        public UserInfo(
                Long id,
                String username,
                String nickname,
                String avatar,
                int level,
                long totalScore) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.avatar = avatar;
            this.level = level;
            this.totalScore = totalScore;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public long getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(long totalScore) {
            this.totalScore = totalScore;
        }
    }
}
