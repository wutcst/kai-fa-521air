package com.snake.dto;

/**
 * 认证响应
 */
public class AuthResponse {
    private String token;
    private UserInfo user;

    public AuthResponse() {}

    public AuthResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    /**
     * 返回给前端的用户信息（不含密码）
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private int level;
        private long totalScore;

        public UserInfo() {}

        public UserInfo(Long id, String username, String nickname, String avatar, int level, long totalScore) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.avatar = avatar;
            this.level = level;
            this.totalScore = totalScore;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public long getTotalScore() { return totalScore; }
        public void setTotalScore(long totalScore) { this.totalScore = totalScore; }
    }
}
