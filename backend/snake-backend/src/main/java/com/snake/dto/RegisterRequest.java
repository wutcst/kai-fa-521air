package com.snake.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 注册请求
 */
@Schema(description = "注册请求")
public class RegisterRequest {
    @Schema(description = "用户名（3-50个字符）", example = "newplayer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码（至少6位）", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "昵称（可选，默认为用户名）", example = "NewPlayer")
    private String nickname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
